package com.company.sportseq.vo;

/**
 * 器材使用率排行：按借用次数统计（横向条形图）。
 */
public record EquipmentUsageVO(
        String equipmentCode,
        String equipmentName,
        String categoryName,
        long borrowCount
) {
}
