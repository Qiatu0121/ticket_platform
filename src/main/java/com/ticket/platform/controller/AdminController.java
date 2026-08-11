package com.ticket.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.platform.common.Result;
import com.ticket.platform.entity.Activity;
import com.ticket.platform.entity.ActivityTicket;
import com.ticket.platform.entity.Order;
import com.ticket.platform.mapper.ActivityMapper;
import com.ticket.platform.mapper.ActivityTicketMapper;
import com.ticket.platform.mapper.OrderMapper;
import com.ticket.platform.service.OrderService;
import com.ticket.platform.state.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理端接口（后台：发布活动、设置票种）
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final ActivityMapper activityMapper;
    private final ActivityTicketMapper ticketMapper;
    private final OrderMapper orderMapper;
    private final OrderService orderService;

    @PostMapping("/activity")
    public Result<Long> createActivity(@RequestBody Activity activity) {
        activity.setCreateTime(LocalDateTime.now());
        activityMapper.insert(activity);
        return Result.ok(activity.getId());
    }

    /**
     * 给活动添加票种：插库后把库存同步初始化到 Redis（抢票预扣的计数源）
     */
    @PostMapping("/activity/{activityId}/ticket")
    public Result<Long> addTicket(@PathVariable Long activityId, @RequestBody ActivityTicket ticket) {
        ticket.setActivityId(activityId);
        ticket.setCreateTime(LocalDateTime.now());
        ticketMapper.insert(ticket);
        orderService.initStock(ticket.getId());
        return Result.ok(ticket.getId());
    }

    /**
     * 订单列表（管理端核销用）：status 传枚举名，如 "PAID"；为空则查全部
     */
    @GetMapping("/orders")
    public Result<List<Order>> listOrders(@RequestParam(required = false) String status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, OrderStatus.valueOf(status));
        }
        wrapper.orderByDesc(Order::getCreateTime);
        return Result.ok(orderMapper.selectList(wrapper));
    }
}
