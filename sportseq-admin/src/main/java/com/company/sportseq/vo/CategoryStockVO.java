package com.company.sportseq.vo;

/**
 * 库存分布：各分类在库器材数量（饼图）。
 */
public record CategoryStockVO(
        String categoryName,
        long quantity
) {
}
