package com.ticket.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`user`")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录名，唯一 */
    private String username;

    private String phone;

    private String nickname;

    /** 角色：USER（普通用户）/ ADMIN（管理员，root） */
    private String role;

    /** BCrypt 加盐哈希，绝不存明文密码 */
    private String passwordHash;

    private LocalDateTime createTime;
}
