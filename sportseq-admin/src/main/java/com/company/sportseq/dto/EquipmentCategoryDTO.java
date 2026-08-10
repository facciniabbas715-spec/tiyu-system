package com.company.sportseq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EquipmentCategoryDTO {

    private Long id;
    private Long parentId;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称长度不能超过50")
    private String categoryName;

    @NotBlank(message = "分类编码不能为空")
    @Size(max = 20, message = "分类编码长度不能超过20")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "分类编码仅允许字母和数字")
    private String categoryCode;

    private String icon;
    private Integer sortOrder;
    private Integer status;
    private String remark;
}
