package com.aicompanion.model.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

/**
 * 修改用户信息 DTO（部分更新，所有字段均为可选）
 */
@Data
public class UpdateUserDTO {

    private String username;

    private String password;

    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String phone;

    private String profession;

    private String bio;
}
