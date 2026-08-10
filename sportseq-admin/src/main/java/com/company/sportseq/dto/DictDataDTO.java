package com.company.sportseq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DictDataDTO {

    private Long id;

    @NotBlank(message = "字典类型不能为空")
    private String dictType;

    private Integer dictSort;

    @NotBlank(message = "字典标签不能为空")
    @Size(max = 100, message = "字典标签长度不能超过100")
    private String dictLabel;

    @NotBlank(message = "字典键值不能为空")
    @Size(max = 100, message = "字典键值长度不能超过100")
    private String dictValue;

    private String cssClass;
    private String listClass;
    private String isDefault;
    private Integer status;
    private String remark;
}
