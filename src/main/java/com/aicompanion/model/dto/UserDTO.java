package com.aicompanion.model.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

/**
 * 更新用户信息 DTO
 */
@Data
public class UserDTO {

    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String phone;

    private String avatar;

    private String profession;

    private String bio;
}
