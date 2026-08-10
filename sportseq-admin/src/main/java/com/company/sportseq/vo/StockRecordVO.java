package com.company.sportseq.vo;

import java.time.LocalDateTime;

public record StockRecordVO(
        Long id,
        String equipmentCode,
        String equipmentName,
        String warehouseName,
        Integer changeType,
        Integer changeQuantity,
        Integer beforeQuantity,
        Integer afterQuantity,
        String refOrderNo,
        String remark,
        LocalDateTime createTime
) {
}
