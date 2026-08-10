package com.company.sportseq.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.io.Serializable;
import java.util.List;

/**
 * 统一分页响应结构。
 */
public record PageResult<T>(long total, long current, long size, List<T> records) implements Serializable {

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }
}
