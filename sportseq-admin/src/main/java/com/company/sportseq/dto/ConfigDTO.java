package com.company.sportseq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConfigDTO {

    private Long id;

    @NotBlank(message = "参数名称不能为空")
    @Size(max = 100, message = "参数名称长度不能超过100")
    private String configName;

    @NotBlank(message = "参数键不能为空")
    @Size(max = 100, message = "参数键长度不能超过100")
    private String configKey;

    @NotBlank(message = "参数值不能为空")
    @Size(max = 500, message = "参数值长度不能超过500")
    private String configValue;

    private String configType;
    private String remark;
}
