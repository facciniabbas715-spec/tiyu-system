package com.company.sportseq.vo;

import java.math.BigDecimal;

public record ReturnItemVO(
        Long id,
        Long borrowItemId,
        String equipmentCode,
        String equipmentName,
        Integer quantity,
        Integer conditionStatus,
        String damageDesc,
        Integer isOverdue,
        Integer overdueDays,
        BigDecimal penaltyAmount
) {
}
