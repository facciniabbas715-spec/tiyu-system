package com.company.sportseq.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ScrapOrderVO(
        Long id,
        String orderNo,
        Long warehouseId,
        String warehouseName,
        Integer scrapType,
        Integer totalQuantity,
        BigDecimal totalLossAmount,
        Integer status,
        String auditRemark,
        Integer disposeMethod,
        String remark,
        LocalDateTime createTime,
        List<ScrapItemVO> items
) {
}
