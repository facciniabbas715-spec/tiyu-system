package com.company.sportseq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.sportseq.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("warehouse")
public class Warehouse extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String warehouseCode;
    private String warehouseName;
    private Long manager;
    private String phone;
    private String address;
    private Integer status;
    private String remark;
}
