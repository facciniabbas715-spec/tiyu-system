package com.company.sportseq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WarehouseDTO {

    private Long id;

    @NotBlank(message = "仓库编码不能为空")
    @Size(max = 20, message = "仓库编码长度不能超过20")
    private String warehouseCode;

    @NotBlank(message = "仓库名称不能为空")
    @Size(max = 50, message = "仓库名称长度不能超过50")
    private String warehouseName;

    private Long manager;
    private String phone;
    private String address;
    private Integer status;
    private String remark;
}
