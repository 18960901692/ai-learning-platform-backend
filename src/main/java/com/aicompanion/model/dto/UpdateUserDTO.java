package com.aicompanion.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理员修改用户信息 DTO
 */
@Data
public class UpdateUserDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    private String password;

    private String nickname;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    private String phone;

    /**
     * 角色：STUDENT / TEACHER / ADMIN
     */
    private String role;
}
