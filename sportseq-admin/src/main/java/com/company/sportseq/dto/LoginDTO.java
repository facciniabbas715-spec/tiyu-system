package com.company.sportseq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过50")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(max = 100, message = "密码长度不能超过100")
    private String password;

    @NotBlank(message = "验证码不能为空")
    @Size(max = 6, message = "验证码格式不正确")
    private String code;

    @NotBlank(message = "验证码标识不能为空")
    private String uuid;
}
