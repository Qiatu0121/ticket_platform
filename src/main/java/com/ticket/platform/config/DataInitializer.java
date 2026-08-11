package com.ticket.platform.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.platform.entity.User;
import com.ticket.platform.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 启动时初始化 root 管理员：
 * 用户名 root / 密码 root123 / 角色 ADMIN，负责管理端活动发布。
 * 已存在则跳过（幂等），密码用 BCrypt 加盐，绝不落明文。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, "root"));
        if (count != null && count > 0) {
            return;
        }

        User root = new User();
        root.setUsername("root");
        root.setPhone("13900000000");
        root.setNickname("系统管理员");
        root.setRole("ADMIN");
        root.setPasswordHash(passwordEncoder.encode("root123"));
        root.setCreateTime(LocalDateTime.now());
        userMapper.insert(root);
        log.info("已创建 root 管理员账号（root / root123）");
    }
}
