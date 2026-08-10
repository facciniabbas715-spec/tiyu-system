package com.company.sportseq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("scrap_item")
public class ScrapItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long scrapId;
    private Long equipmentId;
    private Integer quantity;
    private String scrapReason;
    private BigDecimal lossAmount;
    private String remark;
}
