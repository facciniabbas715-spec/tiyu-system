package com.company.sportseq.vo;

/**
 * 各仓库库存量（柱状图）。
 */
public record WarehouseStockVO(
        String warehouseName,
        long quantity
) {
}
