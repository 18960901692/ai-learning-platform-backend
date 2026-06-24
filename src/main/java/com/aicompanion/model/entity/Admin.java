package com.aicompanion.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 管理员实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("admin")
public class Admin extends BaseEntity {

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
    private LocalDateTime refreshTokenExpireTime;

    /**
     * 逻辑删除标记：0-未删除 1-已删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * admin 表没有 update_time 字段，排除父类的 updateTime
     */
    @TableField(exist = false)
    private LocalDateTime updateTime;
}
