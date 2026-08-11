package com.ticket.platform.state;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.ticket.platform.common.BizException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 订单状态机：
 *
 *   WAIT_PAY(待支付) ──▶ PAID(已支付) ──▶ VERIFIED(已核销)
 *        │                   │
 *        ▼                   ▼
 *   CANCELED(已取消)     REFUNDED(已退款)
 *
 * 任何状态跳转都先走 assertCanTo，非法流转直接抛业务异常，
 * 防止出现"已发货还能取消"这类脏数据。
 */
public enum OrderStatus {

    WAIT_PAY(0, "待支付"),
    PAID(1, "已支付"),
    CANCELED(2, "已取消"),
    VERIFIED(3, "已核销"),
    REFUNDED(4, "已退款");

    /** @EnumValue：MyBatis-Plus 落库时存 code */
    @EnumValue
    private final int code;

    private final String desc;

    OrderStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /** 合法跳转表：key = 当前状态，value = 允许到达的状态 */
    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = new HashMap<>();

    static {
        TRANSITIONS.put(WAIT_PAY, Set.of(PAID, CANCELED));
        TRANSITIONS.put(PAID, Set.of(VERIFIED, REFUNDED));
        TRANSITIONS.put(VERIFIED, Set.of());
        TRANSITIONS.put(CANCELED, Set.of());
        TRANSITIONS.put(REFUNDED, Set.of());
    }

    /**
     * 校验从当前状态能否流转到 target。
     * 幂等场景注意：外部常先判当前状态，避免把"已取消/已支付"再流转一次。
     */
    public void assertCanTo(OrderStatus target) {
        if (!TRANSITIONS.getOrDefault(this, Set.of()).contains(target)) {
            throw new BizException("非法状态流转：" + this.name() + " -> " + target.name());
        }
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
