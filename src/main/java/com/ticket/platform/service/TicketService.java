package com.ticket.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticket.platform.entity.Activity;
import com.ticket.platform.entity.ActivityTicket;
import com.ticket.platform.mapper.ActivityMapper;
import com.ticket.platform.mapper.ActivityTicketMapper;
import com.ticket.platform.vo.ActivityVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 活动详情缓存 —— 面试点：缓存击穿防护（互斥锁）
 *
 * 放票瞬间活动页访问量最大，如果此刻缓存刚好过期，所有请求会同时打到数据库（击穿）。
 * 方案：setIfAbsent 拿互斥锁，只让一个请求重建缓存，其余请求等 50ms 后重试。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private static final String DETAIL_KEY = "activity:detail:";
    private static final String LOCK_KEY = "activity:lock:";
    private static final int MAX_RETRY = 3;

    private final StringRedisTemplate redisTemplate;
    private final ActivityMapper activityMapper;
    private final ActivityTicketMapper ticketMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ActivityVO getActivityDetail(Long activityId) throws Exception {
        String key = DETAIL_KEY + activityId;
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return objectMapper.readValue(cached, ActivityVO.class);
        }
        return loadAndCache(activityId, key, 0);
    }

    private ActivityVO loadAndCache(Long activityId, String key, int retry) throws Exception {
        String lockKey = LOCK_KEY + activityId;

        // 互斥锁：拿不到就等 50ms 重试，避免多请求同时打库
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(5));
        if (Boolean.TRUE.equals(locked)) {
            try {
                // 双检：持锁期间可能已被其他请求重建
                String cached = redisTemplate.opsForValue().get(key);
                if (cached != null) {
                    return objectMapper.readValue(cached, ActivityVO.class);
                }
                ActivityVO vo = queryDb(activityId);
                // 过期时间加随机值，顺带防缓存雪崩
                long ttl = 10 + ThreadLocalRandom.current().nextInt(5);
                redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(vo), Duration.ofMinutes(ttl));
                return vo;
            } finally {
                redisTemplate.delete(lockKey);
            }
        }

        if (retry >= MAX_RETRY) {
            log.warn("缓存重建锁等待超限，直接查库 activityId={}", activityId);
            return queryDb(activityId);
        }
        Thread.sleep(50);
        return loadAndCache(activityId, key, retry + 1);
    }

    private ActivityVO queryDb(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            return null;
        }
        List<ActivityTicket> tickets = ticketMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ActivityTicket>()
                        .eq(ActivityTicket::getActivityId, activityId));
        return new ActivityVO(activity, tickets);
    }
}
