package com.company.sportseq.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 逾期明细：用于报表列表展示与 Excel 导出。
 */
public record OverdueItemVO(
        String returnOrderNo,
        String borrowOrderNo,
        String equipmentCode,
        String equipmentName,
        Integer quantity,
        Integer overdueDays,
        BigDecimal penaltyAmount,
        LocalDateTime confirmTime
) {
}
