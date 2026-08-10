package com.company.sportseq.vo;

import java.util.List;

public record EquipmentCategoryVO(
        Long id,
        Long parentId,
        String categoryName,
        String categoryCode,
        String icon,
        Integer sortOrder,
        Integer status,
        List<EquipmentCategoryVO> children
) {
}
