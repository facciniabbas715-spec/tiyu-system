package com.company.sportseq.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.company.sportseq.entity.SysDept;
import com.company.sportseq.entity.SysRole;
import com.company.sportseq.mapper.SysDeptMapper;
import com.company.sportseq.mapper.SysRoleMapper;
import com.company.sportseq.security.LoginUser;
import com.company.sportseq.security.SecurityUtils;
import com.company.sportseq.service.DataScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据权限实现：取当前用户全部角色的最小 data_scope（1最宽 -> 4最窄），
 * 内置 admin 角色恒为全部。
 */
@Service
@RequiredArgsConstructor
public class DataScopeServiceImpl implements DataScopeService {

    public static final int SCOPE_ALL = 1;
    public static final int SCOPE_DEPT = 2;
    public static final int SCOPE_DEPT_AND_CHILDREN = 3;
    public static final int SCOPE_SELF = 4;

    private final SysRoleMapper roleMapper;
    private final SysDeptMapper deptMapper;

    @Override
    public boolean isFullScope() {
        LoginUser user = SecurityUtils.getLoginUser();
        if (user.getRoles().contains("admin")) {
            return true;
        }
        return scopeOf(user) == SCOPE_ALL;
    }

    @Override
    public boolean isSelfOnly() {
        return !isFullScope() && scopeOf(SecurityUtils.getLoginUser()) == SCOPE_SELF;
    }

    @Override
    public Set<Long> allowedDeptIds() {
        LoginUser user = SecurityUtils.getLoginUser();
        int scope = scopeOf(user);
        if (scope == SCOPE_DEPT) {
            return user.getDeptId() == null ? Set.of() : Set.of(user.getDeptId());
        }
        if (scope == SCOPE_DEPT_AND_CHILDREN) {
            return deptAndChildren(user.getDeptId());
        }
        return Set.of();
    }

    @Override
    public Long currentUserId() {
        return SecurityUtils.getUserId();
    }

    private int scopeOf(LoginUser user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return SCOPE_SELF;
        }
        List<SysRole> roles = roleMapper.selectList(
                Wrappers.<SysRole>lambdaQuery().in(SysRole::getRoleKey, user.getRoles()));
        return roles.stream()
                .map(SysRole::getDataScope)
                .filter(scope -> scope != null)
                .min(Integer::compareTo)
                .orElse(SCOPE_SELF);
    }

    private Set<Long> deptAndChildren(Long deptId) {
        if (deptId == null) {
            return Set.of();
        }
        Set<Long> result = new HashSet<>();
        result.add(deptId);
        List<SysDept> depts = deptMapper.selectList(null);
        String target = String.valueOf(deptId);
        for (SysDept dept : depts) {
            if (dept.getAncestors() == null) {
                continue;
            }
            Set<String> segments = Arrays.stream(dept.getAncestors().split(","))
                    .map(String::trim)
                    .collect(Collectors.toSet());
            if (segments.contains(target)) {
                result.add(dept.getId());
            }
        }
        return result;
    }
}
