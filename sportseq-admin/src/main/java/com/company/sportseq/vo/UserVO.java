package com.company.sportseq.vo;

import java.time.LocalDateTime;
import java.util.List;

public record UserVO(
        Long id,
        Long deptId,
        String deptName,
        String username,
        String nickname,
        String realName,
        String phone,
        String email,
        Integer gender,
        String userType,
        Integer status,
        LocalDateTime loginDate,
        List<Long> roleIds
) {
}
