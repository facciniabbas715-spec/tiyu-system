package com.company.sportseq.service;

import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.dto.RoleDTO;
import com.company.sportseq.vo.RoleVO;

import java.util.List;

public interface SysRoleService {

    PageResult<RoleVO> page(long current, long size, String roleName, Integer status);

    void add(RoleDTO dto);

    void update(RoleDTO dto);

    void remove(Long id);

    List<Long> listMenuIds(Long roleId);

    void assignMenus(Long roleId, List<Long> menuIds);
}
