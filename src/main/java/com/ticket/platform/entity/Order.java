package com.ticket.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ticket.platform.state.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("`order`")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    private Long activityId;

    private Long ticketId;

    private Integer count;

    private BigDecimal amount;

    /** 订单状态：由状态机 OrderStatus 管理 */
    private OrderStatus status;

    private LocalDateTime createTime;

    private LocalDateTime payTime;
}
