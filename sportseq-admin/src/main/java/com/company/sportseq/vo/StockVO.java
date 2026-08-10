package com.company.sportseq.vo;

public record StockVO(
        Long equipmentId,
        String equipmentCode,
        String equipmentName,
        String categoryName,
        String unit,
        Long warehouseId,
        String warehouseName,
        Integer quantity,
        Integer lockedQuantity,
        Integer safeStock,
        boolean warning
) {
}
