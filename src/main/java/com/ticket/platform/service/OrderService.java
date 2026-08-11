package com.ticket.platform.service;

import com.ticket.platform.common.BizException;
import com.ticket.platform.entity.ActivityTicket;
import com.ticket.platform.entity.Order;
import com.ticket.platform.entity.OrderLog;
import com.ticket.platform.mapper.ActivityTicketMapper;
import com.ticket.platform.mapper.OrderLogMapper;
import com.ticket.platform.mapper.OrderMapper;
import com.ticket.platform.state.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 1. 防超卖：Redis Lua 原子预扣 + 数据库乐观锁兜底
 2. 幂等：重复抢票由 t_order 唯一索引 (user_id, activity_id) 兜底
 3. 状态机：所有流转都过 assertCanTo
 4. 可选：Redis 计数器限流
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    public static final String STOCK_KEY_PREFIX = "ticket:stock:";
    private static final String RATE_KEY_PREFIX = "rate:activity:";
    private static final DateTimeFormatter ORDER_NO_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final DefaultRedisScript<Long> DEC_STOCK_SCRIPT = loadScript("lua/stock_dec.lua");
    private static final DefaultRedisScript<Long> INC_STOCK_SCRIPT = loadScript("lua/stock_inc.lua");

    private final ActivityTicketMapper activityTicketMapper;
    private final OrderMapper orderMapper;
    private final OrderLogMapper orderLogMapper;
    private final StringRedisTemplate redisTemplate;

    // ===================== 抢票（核心） =====================

    /**
     * 抢票主流程。放票瞬间大量并发打进来时：
     *  ① Redis Lua 原子预扣（热点路径，扛并发）
     *  ② 数据库乐观锁兜底（双保险，防 Redis 与 DB 不一致）
     *  ③ 插入订单（待支付）
     *  失败则 Redis 回补库存，DB 侧由事务回滚。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long grabTicket(Long userId, Long ticketId) {
        ActivityTicket ticket = activityTicketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new BizException("票种不存在");
        }
        // 可选：单用户限流（放票瞬间防止脚本机器人狂点）
        checkRate(ticket.getActivityId(), userId);

        // ① Redis Lua 原子预扣
        String stockKey = STOCK_KEY_PREFIX + ticketId;
        Long dec = redisTemplate.execute(DEC_STOCK_SCRIPT, List.of(stockKey));
        if (dec == null || dec.longValue() != 1L) {
            throw new BizException("票已抢完，手慢了~");
        }

        try {
            // ② 数据库乐观锁兜底：返回 0 = 库存不足
            int updated = activityTicketMapper.deductStock(ticketId, 1);
            if (updated == 0) {
                throw new BizException("票已抢完，手慢了~");
            }

            // ③ 生成订单（待支付）
            Order order = new Order();
            order.setOrderNo(generateOrderNo());
            order.setUserId(userId);
            order.setActivityId(ticket.getActivityId());
            order.setTicketId(ticketId);
            order.setCount(1);
            order.setAmount(ticket.getPrice());
            order.setStatus(OrderStatus.WAIT_PAY);
            order.setCreateTime(LocalDateTime.now());
            // ④ 重复抢票会撞唯一索引 uk_user_activity，抛 DuplicateKeyException → 走 catch 回补
            orderMapper.insert(order);

            saveLog(order.getId(), null, OrderStatus.WAIT_PAY);
            return order.getId();
        } catch (Exception e) {
            // ⑤ 任何失败都回补 Redis 库存；DB 的扣减由 @Transactional 回滚
            redisTemplate.execute(INC_STOCK_SCRIPT, List.of(stockKey));
            log.warn("抢票失败 userId={} ticketId={}", userId, ticketId, e);
            if (e instanceof BizException) {
                throw (BizException) e;
            }
            throw new BizException("抢票失败，请重试");
        }
    }

    // ===================== 状态流转 =====================

    /** 模拟支付：WAIT_PAY -> PAID */
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long orderId, Long userId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BizException("订单不存在");
        }
        if (order.getStatus() != OrderStatus.WAIT_PAY) {
            throw new BizException("当前状态不可支付");
        }
        order.getStatus().assertCanTo(OrderStatus.PAID);

        Order update = new Order();
        update.setId(orderId);
        update.setStatus(OrderStatus.PAID);
        update.setPayTime(LocalDateTime.now());
        orderMapper.updateById(update);

        saveLog(orderId, OrderStatus.WAIT_PAY, OrderStatus.PAID);
    }

    /** 入场核销：PAID -> VERIFIED（管理员操作） */
    @Transactional(rollbackFor = Exception.class)
    public void verifyOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        order.getStatus().assertCanTo(OrderStatus.VERIFIED);

        Order update = new Order();
        update.setId(orderId);
        update.setStatus(OrderStatus.VERIFIED);
        orderMapper.updateById(update);

        saveLog(orderId, order.getStatus(), OrderStatus.VERIFIED);
    }

    /**
     * 超时关单（定时任务调用）：WAIT_PAY -> CANCELED，并回补库存。
     * 幂等：状态已不是待支付则直接返回。
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelTimeoutOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != OrderStatus.WAIT_PAY) {
            return; // 已取消 / 已支付，无需处理
        }
        order.getStatus().assertCanTo(OrderStatus.CANCELED);

        Order update = new Order();
        update.setId(orderId);
        update.setStatus(OrderStatus.CANCELED);
        orderMapper.updateById(update);

        // 回补库存（DB + Redis 双回补，保证两边一致）
        activityTicketMapper.incrementStock(order.getTicketId(), order.getCount());
        redisTemplate.opsForValue().increment(STOCK_KEY_PREFIX + order.getTicketId());

        saveLog(orderId, OrderStatus.WAIT_PAY, OrderStatus.CANCELED);
        log.info("超时关单 orderId={}", orderId);
    }

    // ===================== 辅助方法 =====================

    /** 发布票种后调用：把库存初始化到 Redis，作为抢票时的预扣计数 */
    public void initStock(Long ticketId) {
        ActivityTicket ticket = activityTicketMapper.selectById(ticketId);
        if (ticket != null) {
            redisTemplate.opsForValue().set(STOCK_KEY_PREFIX + ticketId, String.valueOf(ticket.getStock()));
        }
    }

    private void checkRate(Long activityId, Long userId) {
        String key = RATE_KEY_PREFIX + activityId + ":" + userId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofSeconds(1)); // 1 秒窗口
        }
        if (count != null && count > 10) {
            throw new BizException("操作过于频繁，请稍后再试");
        }
    }

    private void saveLog(Long orderId, OrderStatus from, OrderStatus to) {
        OrderLog logEntry = new OrderLog();
        logEntry.setOrderId(orderId);
        logEntry.setFromStatus(from == null ? null : from.name());
        logEntry.setToStatus(to.name());
        logEntry.setCreateTime(LocalDateTime.now());
        orderLogMapper.insert(logEntry);
    }

    private String generateOrderNo() {
        return LocalDateTime.now().format(ORDER_NO_FMT) + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private static DefaultRedisScript<Long> loadScript(String classpath) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource(classpath)));
        script.setResultType(Long.class);
        return script;
    }
}
