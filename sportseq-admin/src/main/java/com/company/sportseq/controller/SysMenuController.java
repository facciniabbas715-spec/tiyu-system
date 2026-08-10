package com.company.sportseq.controller;

import com.company.sportseq.common.result.Result;
import com.company.sportseq.annotation.Log;
import com.company.sportseq.dto.MenuDTO;
import com.company.sportseq.service.SysMenuService;
import com.company.sportseq.vo.MenuVO;
import com.company.sportseq.vo.RouterVO;
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
@RequestMapping("/api/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService menuService;

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:menu:list')")
    public Result<List<MenuVO>> tree(@RequestParam(required = false) String menuName) {
        return Result.success(menuService.tree(menuName));
    }

    @GetMapping("/routers")
    public Result<List<RouterVO>> routers() {
        return Result.success(menuService.routers());
    }

    @PostMapping
    @Log(title = "菜单管理", businessType = 1)
    @PreAuthorize("hasAuthority('system:menu:add')")
    public Result<Void> add(@Valid @RequestBody MenuDTO dto) {
        menuService.add(dto);
        return Result.success();
    }

    @PutMapping
    @Log(title = "菜单管理", businessType = 2)
    @PreAuthorize("hasAuthority('system:menu:edit')")
    public Result<Void> update(@Valid @RequestBody MenuDTO dto) {
        menuService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Log(title = "菜单管理", businessType = 3)
    @PreAuthorize("hasAuthority('system:menu:remove')")
    public Result<Void> remove(@PathVariable Long id) {
        menuService.remove(id);
        return Result.success();
    }
}
