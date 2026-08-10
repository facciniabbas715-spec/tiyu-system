package com.company.sportseq.vo;

import java.math.BigDecimal;

public record StockInItemVO(
        Long id,
        Long equipmentId,
        String equipmentCode,
        String equipmentName,
        String unit,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal amount,
        String remark
) {
}
