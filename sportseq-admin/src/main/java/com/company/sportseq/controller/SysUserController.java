package com.company.sportseq.controller;

import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.common.result.Result;
import com.company.sportseq.annotation.Log;
import com.company.sportseq.dto.ResetPasswordDTO;
import com.company.sportseq.dto.UserDTO;
import com.company.sportseq.dto.UserStatusDTO;
import com.company.sportseq.service.SysUserService;
import com.company.sportseq.vo.UserVO;
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

@RestController
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<PageResult<UserVO>> page(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String username,
                                           @RequestParam(required = false) String phone,
                                           @RequestParam(required = false) Integer status,
                                           @RequestParam(required = false) Long deptId) {
        return Result.success(userService.page(current, size, username, phone, status, deptId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<UserVO> detail(@PathVariable Long id) {
        return Result.success(userService.detail(id));
    }

    @PostMapping
    @Log(title = "用户管理", businessType = 1)
    @PreAuthorize("hasAuthority('system:user:add')")
    public Result<Void> add(@Valid @RequestBody UserDTO dto) {
        userService.add(dto);
        return Result.success();
    }

    @PutMapping
    @Log(title = "用户管理", businessType = 2)
    @PreAuthorize("hasAuthority('system:user:edit')")
    public Result<Void> update(@Valid @RequestBody UserDTO dto) {
        userService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Log(title = "用户管理", businessType = 3)
    @PreAuthorize("hasAuthority('system:user:remove')")
    public Result<Void> remove(@PathVariable Long id) {
        userService.remove(id);
        return Result.success();
    }

    @PutMapping("/resetPassword")
    @Log(title = "重置密码", businessType = 2)
    @PreAuthorize("hasAuthority('system:user:resetPwd')")
    public Result<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        userService.resetPassword(dto);
        return Result.success();
    }

    @PutMapping("/status")
    @Log(title = "用户状态", businessType = 2)
    @PreAuthorize("hasAuthority('system:user:edit')")
    public Result<Void> changeStatus(@Valid @RequestBody UserStatusDTO dto) {
        userService.changeStatus(dto);
        return Result.success();
    }
}
