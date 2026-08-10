package com.company.sportseq.vo;

import java.util.List;

public record MenuVO(
        Long id,
        Long parentId,
        String menuName,
        Integer orderNum,
        String menuType,
        String path,
        String component,
        String perms,
        String icon,
        Integer visible,
        Integer status,
        List<MenuVO> children
) {
}
