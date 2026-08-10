package com.company.sportseq.service;

import java.util.Set;

/**
 * 数据权限服务：根据当前用户角色 data_scope 计算可见数据范围。
 * 1全部 / 2本部门 / 3本部门及以下 / 4仅本人。
 */
public interface DataScopeService {

    /** 是否拥有全部数据权限（data_scope=1 或内置 admin 角色） */
    boolean isFullScope();

    /** 是否仅本人数据（data_scope=4） */
    boolean isSelfOnly();

    /** 允许访问的部门ID集合（本部门/本部门及以下），仅本人时返回空集 */
    Set<Long> allowedDeptIds();

    /** 当前登录用户ID */
    Long currentUserId();
}
