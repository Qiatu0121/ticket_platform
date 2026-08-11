package com.ticket.platform.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.platform.entity.Order;
import com.ticket.platform.mapper.OrderMapper;
import com.ticket.platform.service.OrderService;
import com.ticket.platform.state.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时自动关单：
 * 每 30 秒扫一次"待支付且超过 15 分钟"的订单，取消并回补库存。
 * 兜底方案：生产上量大时可换 RocketMQ 延迟消息，但定时任务简单可靠、够用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private static final long TIMEOUT_MINUTES = 15;
    private static final int BATCH_SIZE = 200;

    private final OrderMapper orderMapper;
    private final OrderService orderService;

    @Scheduled(cron = "0/30 * * * * ?")   // 每 30 秒
    public void closeTimeoutOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);
        List<Order> timeoutOrders = orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getStatus, OrderStatus.WAIT_PAY)
                        .lt(Order::getCreateTime, deadline)
                        .last("limit " + BATCH_SIZE));

        if (timeoutOrders.isEmpty()) {
            return;
        }
        log.info("扫描到 {} 笔超时未支付订单，开始关单", timeoutOrders.size());
        for (Order order : timeoutOrders) {
            try {
                orderService.cancelTimeoutOrder(order.getId());
            } catch (Exception e) {
                // 单笔失败不影响其他订单，记录后继续
                log.error("关单失败 orderId={}", order.getId(), e);
            }
        }
    }
}
