package com.company.sportseq.vo;

import java.util.List;

public record DeptVO(
        Long id,
        Long parentId,
        String deptName,
        Integer orderNum,
        String leader,
        String phone,
        String email,
        Integer status,
        List<DeptVO> children
) {
}
