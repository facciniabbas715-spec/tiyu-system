package com.company.sportseq.vo;

import java.time.LocalDateTime;

public record RoleVO(
        Long id,
        String roleName,
        String roleKey,
        Integer roleSort,
        Integer dataScope,
        Integer status,
        String remark,
        LocalDateTime createTime
) {
}
