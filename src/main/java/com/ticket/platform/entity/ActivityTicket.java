package com.ticket.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("`activity_ticket`")
public class ActivityTicket {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long activityId;

    /** 票种名：普通票 / VIP 票 / 学生票 */
    private String name;

    private BigDecimal price;

    private Integer totalStock;

    /** 当前余票：抢票时被 Redis 预扣 + 数据库乐观锁双重保护 */
    private Integer stock;

    private LocalDateTime createTime;
}
