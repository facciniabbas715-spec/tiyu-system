package com.company.sportseq.vo;

import java.util.Set;

public record UserInfoVO(
        Long userId,
        String username,
        String realName,
        String avatar,
        String userType,
        Set<String> permissions,
        Set<String> roles
) {
}
