package com.ticket.platform.common;

/**
 * 业务异常：抢票失败、状态非法等，由全局异常处理器转成友好提示
 */
public class BizException extends RuntimeException {

    public BizException(String message) {
        super(message);
    }
}
