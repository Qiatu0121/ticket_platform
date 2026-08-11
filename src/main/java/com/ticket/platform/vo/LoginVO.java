package com.ticket.platform.vo;

import com.ticket.platform.entity.User;
import lombok.Data;

/**
 * 登录成功返回体：{ token, user }
 * user 已剥掉 passwordHash，不会把哈希泄露给前端。
 */
@Data
public class LoginVO {

    private String token;

    private User user;

    public static LoginVO of(String token, User user) {
        LoginVO vo = new LoginVO();
        vo.token = token;
        vo.user = user;
        return vo;
    }
}
