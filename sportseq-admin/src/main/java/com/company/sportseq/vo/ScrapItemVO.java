package com.company.sportseq.vo;

import java.math.BigDecimal;

public record ScrapItemVO(
        Long id,
        Long equipmentId,
        String equipmentCode,
        String equipmentName,
        String unit,
        Integer quantity,
        String scrapReason,
        BigDecimal lossAmount
) {
}
