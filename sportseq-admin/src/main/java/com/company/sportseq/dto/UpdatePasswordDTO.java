package com.company.sportseq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePasswordDTO {

    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 20, message = "新密码长度需为8-20位")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "新密码需同时包含字母和数字")
    private String newPassword;
}
