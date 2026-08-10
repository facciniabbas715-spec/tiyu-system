package com.company.sportseq.vo;

import java.time.LocalDate;

public record BorrowItemVO(
        Long id,
        Long equipmentId,
        String equipmentCode,
        String equipmentName,
        String unit,
        Long warehouseId,
        String warehouseName,
        Integer quantity,
        Integer issuedQuantity,
        Integer returnedQuantity,
        LocalDate expectedReturnDate,
        Integer overdueFlag,
        Integer overdueDays,
        Integer status
) {
}
