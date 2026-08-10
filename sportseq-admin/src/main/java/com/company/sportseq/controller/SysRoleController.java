package com.company.sportseq.controller;

import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.common.result.Result;
import com.company.sportseq.annotation.Log;
import com.company.sportseq.dto.RoleDTO;
import com.company.sportseq.service.SysRoleService;
import com.company.sportseq.vo.RoleVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<PageResult<RoleVO>> page(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String roleName,
                                           @RequestParam(required = false) Integer status) {
        return Result.success(roleService.page(current, size, roleName, status));
    }

    @PostMapping
    @Log(title = "角色管理", businessType = 1)
    @PreAuthorize("hasAuthority('system:role:add')")
    public Result<Void> add(@Valid @RequestBody RoleDTO dto) {
        roleService.add(dto);
        return Result.success();
    }

    @PutMapping
    @Log(title = "角色管理", businessType = 2)
    @PreAuthorize("hasAuthority('system:role:edit')")
    public Result<Void> update(@Valid @RequestBody RoleDTO dto) {
        roleService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Log(title = "角色管理", businessType = 3)
    @PreAuthorize("hasAuthority('system:role:remove')")
    public Result<Void> remove(@PathVariable Long id) {
        roleService.remove(id);
        return Result.success();
    }

    @GetMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<List<Long>> listMenuIds(@PathVariable Long id) {
        return Result.success(roleService.listMenuIds(id));
    }

    @PutMapping("/{id}/menus")
    @Log(title = "角色授权", businessType = 2)
    @PreAuthorize("hasAuthority('system:role:assignMenu')")
    public Result<Void> assignMenus(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        roleService.assignMenus(id, menuIds);
        return Result.success();
    }
}
