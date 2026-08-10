package com.company.sportseq.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EquipmentVO(
        Long id,
        String equipmentCode,
        String equipmentName,
        Long categoryId,
        String categoryName,
        String brand,
        String model,
        String spec,
        String unit,
        BigDecimal purchasePrice,
        Integer safeStock,
        Integer maxBorrowDays,
        String imageUrl,
        Integer status,
        String description,
        LocalDateTime createTime
) {
}
