package com.company.sportseq.vo;

/**
 * 借用趋势：按月统计借用/归还数量（折线图）。
 */
public record BorrowTrendVO(
        String month,
        long borrowQuantity,
        long returnQuantity
) {
}
