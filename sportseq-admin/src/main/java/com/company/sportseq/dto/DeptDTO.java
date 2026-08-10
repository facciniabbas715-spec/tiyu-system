package com.company.sportseq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeptDTO {

    private Long id;

    @NotNull(message = "父部门不能为空")
    private Long parentId;

    @NotBlank(message = "部门名称不能为空")
    @Size(max = 50, message = "部门名称长度不能超过50")
    private String deptName;

    private Integer orderNum;
    private String leader;
    private String phone;
    private String email;
    private Integer status;
}
