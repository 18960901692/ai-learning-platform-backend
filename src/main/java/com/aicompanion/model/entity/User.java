package com.aicompanion.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class User extends BaseEntity {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 职业/专业
     */
    private String profession;

    /**
     * 个人简介
     */
    private String bio;

    /**
     * 角色：STUDENT / TEACHER / ADMIN
     */
    private String role;

    /**
     * 状态：0-禁用 1-正常
     */
    private Integer status;

    /**
     * 记住密码 Token（刷新令牌）
     */
    private String refreshToken;

    /**
     * 刷新令牌过期时间
     */
    private java.time.LocalDateTime refreshTokenExpireTime;
}
