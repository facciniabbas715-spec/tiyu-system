package com.company.sportseq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MenuDTO {

    private Long id;

    @NotNull(message = "父菜单不能为空")
    private Long parentId;

    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 50, message = "菜单名称长度不能超过50")
    private String menuName;

    private Integer orderNum;

    @NotBlank(message = "菜单类型不能为空")
    private String menuType;

    private String path;
    private String component;
    private String query;
    private String perms;
    private String icon;
    private Integer visible;
    private Integer status;
}
