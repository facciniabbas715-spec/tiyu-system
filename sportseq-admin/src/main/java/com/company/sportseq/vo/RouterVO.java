package com.company.sportseq.vo;

import java.util.List;

/**
 * 动态路由节点：meta 承载标题/图标，component 为前端组件路径（Layout 表示目录）。
 */
public record RouterVO(
        String path,
        String component,
        String name,
        Meta meta,
        List<RouterVO> children
) {
    public record Meta(String title, String icon) {
    }
}
