package com.ticket.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.platform.common.BizException;
import com.ticket.platform.common.Result;
import com.ticket.platform.config.AuthInterceptor;
import com.ticket.platform.entity.Order;
import com.ticket.platform.entity.User;
import com.ticket.platform.mapper.OrderMapper;
import com.ticket.platform.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 订单接口：必须登录（由拦截器守卫）。
 * 当前用户统一从拦截器放入的 loginUser 取，不再信任前端传的 userId，避免越权。
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    /** 抢票：核心接口。返回订单 id */
    @PostMapping("/grab")
    public Result<Long> grab(HttpServletRequest request, @RequestParam Long ticketId) {
        return Result.ok(orderService.grabTicket(currentUser(request).getId(), ticketId));
    }

    /** 模拟支付：只能支付自己的订单 */
    @PostMapping("/pay")
    public Result<Void> pay(HttpServletRequest request, @RequestParam Long orderId) {
        orderService.payOrder(orderId, currentUser(request).getId());
        return Result.ok(null);
    }

    /** 入场核销（管理员扫码） */
    @PostMapping("/verify")
    public Result<Void> verify(HttpServletRequest request, @RequestParam Long orderId) {
        User user = currentUser(request);
        if (!"ADMIN".equals(user.getRole())) {
            throw new BizException("无权限，仅管理员可操作");
        }
        orderService.verifyOrder(orderId);
        return Result.ok(null);
    }

    /** 我的订单：只能看自己的 */
    @GetMapping("/my")
    public Result<List<Order>> myOrders(HttpServletRequest request) {
        return Result.ok(orderMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, currentUser(request).getId())
                        .orderByDesc(Order::getCreateTime)));
    }

    /** 从拦截器写入的 loginUser 取当前登录用户 */
    private User currentUser(HttpServletRequest request) {
        return (User) request.getAttribute(AuthInterceptor.LOGIN_USER_ATTR);
    }
}
