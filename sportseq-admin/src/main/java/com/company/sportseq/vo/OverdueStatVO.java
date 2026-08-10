package com.company.sportseq.vo;

import java.math.BigDecimal;

/**
 * 逾期统计汇总：逾期单数、逾期明细数、平均逾期天数、违约金合计。
 */
public record OverdueStatVO(
        long overdueOrderCount,
        long overdueItemCount,
        BigDecimal avgOverdueDays,
        BigDecimal penaltyTotal
) {
}
