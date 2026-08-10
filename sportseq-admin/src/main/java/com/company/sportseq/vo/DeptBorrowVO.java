package com.company.sportseq.vo;

/**
 * 部门借用统计：借用单数、借用数量与在借数量。
 */
public record DeptBorrowVO(
        String deptName,
        long borrowCount,
        long borrowQuantity,
        long outstandingQuantity
) {
}
