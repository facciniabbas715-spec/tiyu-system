package com.company.sportseq.vo;

import java.time.LocalDateTime;
import java.util.List;

public record ReturnOrderVO(
        Long id,
        String orderNo,
        Long borrowOrderId,
        String borrowOrderNo,
        String username,
        Long warehouseId,
        String warehouseName,
        Integer totalQuantity,
        Integer returnType,
        Integer status,
        String confirmRemark,
        String remark,
        LocalDateTime createTime,
        List<ReturnItemVO> items
) {
}
