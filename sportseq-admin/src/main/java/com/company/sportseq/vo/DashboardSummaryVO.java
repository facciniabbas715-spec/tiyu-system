package com.company.sportseq.vo;

import java.math.BigDecimal;

/**
 * 仪表盘汇总：今日业务量、库存预警、报废待审、器材总数与库存总价值。
 */
public record DashboardSummaryVO(
        long todayBorrowCount,
        long todayReturnCount,
        long todayStockInCount,
        long warningStockCount,
        long pendingScrapCount,
        long equipmentTotal,
        BigDecimal stockTotalValue
) {
}
