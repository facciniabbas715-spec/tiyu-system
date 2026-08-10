package com.company.sportseq.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record BorrowOrderVO(
        Long id,
        String orderNo,
        Long userId,
        String username,
        String realName,
        Integer borrowType,
        String purpose,
        LocalDate expectedReturnDate,
        Integer totalQuantity,
        Integer status,
        Integer extendCount,
        String auditRemark,
        String remark,
        LocalDateTime createTime,
        List<BorrowItemVO> items
) {
}
