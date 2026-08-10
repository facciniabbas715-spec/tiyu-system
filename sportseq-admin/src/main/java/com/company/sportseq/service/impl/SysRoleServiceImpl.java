package com.company.sportseq.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.sportseq.common.exception.BizException;
import com.company.sportseq.common.exception.ErrorCode;
import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.dto.RoleDTO;
import com.company.sportseq.entity.SysMenu;
import com.company.sportseq.entity.SysRole;
import com.company.sportseq.entity.SysRoleMenu;
import com.company.sportseq.entity.SysUserRole;
import com.company.sportseq.mapper.SysMenuMapper;
import com.company.sportseq.mapper.SysRoleMapper;
import com.company.sportseq.mapper.SysRoleMenuMapper;
import com.company.sportseq.mapper.SysUserRoleMapper;
import com.company.sportseq.service.SysRoleService;
import com.company.sportseq.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuMapper menuMapper;

    @Override
    public PageResult<RoleVO> page(long current, long size, String roleName, Integer status) {
        Page<SysRole> page = roleMapper.selectPage(new Page<>(current, size),
                Wrappers.<SysRole>lambdaQuery()
                        .like(StrUtil.isNotBlank(roleName), SysRole::getRoleName, roleName)
                        .eq(status != null, SysRole::getStatus, status)
                        .orderByAsc(SysRole::getRoleSort));
        List<RoleVO> records = page.getRecords().stream()
                .map(r -> new RoleVO(r.getId(), r.getRoleName(), r.getRoleKey(), r.getRoleSort(),
                        r.getDataScope(), r.getStatus(), r.getRemark(), r.getCreateTime()))
                .toList();
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(RoleDTO dto) {
        checkRoleKeyUnique(dto.getRoleKey(), null);
        SysRole role = new SysRole();
        role.setRoleName(dto.getRoleName());
        role.setRoleKey(dto.getRoleKey());
        role.setRoleSort(dto.getRoleSort() == null ? 0 : dto.getRoleSort());
        role.setDataScope(dto.getDataScope() == null ? 1 : dto.getDataScope());
        role.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        role.setRemark(dto.getRemark());
        roleMapper.insert(role);
        assignMenus(role.getId(), dto.getMenuIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(RoleDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "角色ID不能为空");
        }
        checkRoleKeyUnique(dto.getRoleKey(), dto.getId());
        SysRole role = new SysRole();
        role.setId(dto.getId());
        role.setRoleName(dto.getRoleName());
        if (dto.getId() == 1L) {
            SysRole existing = roleMapper.selectById(1L);
            if (existing == null) {
                throw new BizException(ErrorCode.PARAM_ERROR, "系统内置角色不存在");
            }
            if (!Objects.equals(existing.getRoleKey(), dto.getRoleKey())
                    || !Objects.equals(existing.getStatus(), dto.getStatus())) {
                throw new BizException(ErrorCode.PARAM_ERROR, "系统内置角色不允许修改角色标识或状态");
            }
            role.setRoleKey(existing.getRoleKey());
            role.setStatus(existing.getStatus());
        } else {
            role.setRoleKey(dto.getRoleKey());
            role.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        }
        role.setRoleSort(dto.getRoleSort() == null ? 0 : dto.getRoleSort());
        role.setDataScope(dto.getDataScope() == null ? 1 : dto.getDataScope());
        role.setRemark(dto.getRemark());
        roleMapper.updateById(role);
        roleMenuMapper.deleteByRoleId(dto.getId());
        assignMenus(dto.getId(), dto.getMenuIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        if (id == 1L) {
            throw new BizException(ErrorCode.PARAM_ERROR, "系统内置角色不允许删除");
        }
        Long userCount = userRoleMapper.selectCount(
                Wrappers.<SysUserRole>lambdaQuery().eq(SysUserRole::getRoleId, id));
        if (userCount > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "角色已分配用户，无法删除");
        }
        roleMapper.deleteById(id);
        roleMenuMapper.deleteByRoleId(id);
    }

    @Override
    public List<Long> listMenuIds(Long roleId) {
        return roleMenuMapper.selectMenuIdsByRoleId(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        if (roleId == 1L) {
            List<Long> allMenuIds = menuMapper.selectList(null).stream()
                    .map(SysMenu::getId)
                    .toList();
            if (menuIds == null || !new HashSet<>(menuIds).containsAll(allMenuIds)) {
                throw new BizException(ErrorCode.PARAM_ERROR, "系统内置角色必须保留全部菜单权限");
            }
        }
        if (menuIds == null) {
            return;
        }
        roleMenuMapper.deleteByRoleId(roleId);
        for (Long menuId : menuIds) {
            SysRoleMenu relation = new SysRoleMenu();
            relation.setRoleId(roleId);
            relation.setMenuId(menuId);
            roleMenuMapper.insert(relation);
        }
    }

    private void checkRoleKeyUnique(String roleKey, Long excludeId) {
        Long count = roleMapper.selectCount(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getRoleKey, roleKey)
                .ne(excludeId != null, SysRole::getId, excludeId));
        if (count > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "角色权限字符已存在");
        }
    }
}
