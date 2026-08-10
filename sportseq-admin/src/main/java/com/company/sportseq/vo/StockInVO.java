package com.company.sportseq.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record StockInVO(
        Long id,
        String orderNo,
        Long warehouseId,
        String warehouseName,
        String supplier,
        Integer inType,
        Integer totalQuantity,
        BigDecimal totalAmount,
        Integer status,
        Long auditBy,
        LocalDateTime auditTime,
        String auditRemark,
        Long receiveBy,
        LocalDateTime receiveTime,
        String remark,
        LocalDateTime createTime,
        List<StockInItemVO> items
) {
}
