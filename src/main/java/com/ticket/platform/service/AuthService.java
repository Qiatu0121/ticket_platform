package com.ticket.platform.service;

import com.ticket.platform.entity.User;
import com.ticket.platform.mapper.UserMapper;
import com.ticket.platform.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

/**
 * 轻量 token 鉴权：登录发一个随机 token 存 Redis（token -> userId），
 * 后续请求带 Authorization: Bearer <token>，拦截器据此还原用户。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String TOKEN_KEY_PREFIX = "auth:token:";
    private static final Duration TOKEN_TTL = Duration.ofDays(7);

    private final StringRedisTemplate redisTemplate;
    private final UserMapper userMapper;

    /** 登录成功：签发 token 并返回 { token, user } */
    public LoginVO login(User user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX + token, String.valueOf(user.getId()), TOKEN_TTL);
        user.setPasswordHash(null); // 不返回密码字段
        return LoginVO.of(token, user);
    }

    /** 根据 token 还原用户；token 不存在 / 已过期 / 用户被删则返回 null */
    public User getUserByToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        String userId = redisTemplate.opsForValue().get(TOKEN_KEY_PREFIX + token);
        if (userId == null) {
            return null;
        }
        return userMapper.selectById(Long.valueOf(userId));
    }

    /** 登出：删除 token */
    public void logout(String token) {
        if (token != null && !token.isEmpty()) {
            redisTemplate.delete(TOKEN_KEY_PREFIX + token);
        }
    }

    /** 从请求头解析 Bearer token，没有则返回 null */
    public static String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
