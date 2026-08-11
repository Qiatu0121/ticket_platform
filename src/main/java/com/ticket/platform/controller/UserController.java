package com.ticket.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.platform.common.BizException;
import com.ticket.platform.common.Result;
import com.ticket.platform.entity.User;
import com.ticket.platform.mapper.UserMapper;
import com.ticket.platform.service.AuthService;
import com.ticket.platform.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserMapper userMapper;
    private final AuthService authService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PostMapping("/register")
    public Result<Long> register(@RequestParam String username,
                                 @RequestParam String phone,
                                 @RequestParam String nickname,
                                 @RequestParam String password) {
        User user = new User();
        user.setUsername(username);
        user.setPhone(phone);
        user.setNickname(nickname);
        user.setRole("USER"); // 注册只能是普通用户，管理员仅 root
        user.setPasswordHash(passwordEncoder.encode(password)); // 加盐哈希，绝不明文
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);
        return Result.ok(user.getId());
    }

    /** 登录：account 支持用户名或手机号，成功返回 { token, user } */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestParam String account,
                                 @RequestParam String password) {
        // 用户名或手机号任一命中即可；最多命中一条，取第一条
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, account)
                        .or()
                        .eq(User::getPhone, account)
                        .last("limit 1"));
        User user = users.isEmpty() ? null : users.get(0);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BizException("用户名/手机号或密码错误");
        }
        return Result.ok(authService.login(user));
    }

    /** 登出：使当前 token 失效 */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        authService.logout(AuthService.resolveToken(request));
        return Result.ok(null);
    }
}
