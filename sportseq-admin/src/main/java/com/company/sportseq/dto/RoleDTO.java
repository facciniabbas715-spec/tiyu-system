package com.company.sportseq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class RoleDTO {

    private Long id;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 50, message = "角色名称长度不能超过50")
    private String roleName;

    @NotBlank(message = "角色权限字符不能为空")
    @Size(max = 50, message = "角色权限字符长度不能超过50")
    @Pattern(regexp = "^[a-zA-Z_][a-zA-Z0-9_]*$", message = "角色权限字符仅允许字母、数字、下划线")
    private String roleKey;

    private Integer roleSort;
    private Integer dataScope;
    private Integer status;
    private String remark;
    private List<Long> menuIds;
}
