package com.company.sportseq.controller;

import com.company.sportseq.annotation.Log;
import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.common.result.Result;
import com.company.sportseq.entity.SysLoginLog;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.sportseq.mapper.SysLoginLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitor/loginlog")
@RequiredArgsConstructor
public class LoginLogController {

    private final SysLoginLogMapper loginLogMapper;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('monitor:loginlog:list')")
    public Result<PageResult<SysLoginLog>> page(@RequestParam(defaultValue = "1") long current,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String userName,
                                                @RequestParam(required = false) Integer status) {
        Page<SysLoginLog> page = loginLogMapper.selectPage(new Page<>(current, size),
                Wrappers.<SysLoginLog>lambdaQuery()
                        .like(userName != null && !userName.isBlank(), SysLoginLog::getUserName, userName)
                        .eq(status != null, SysLoginLog::getStatus, status)
                        .orderByDesc(SysLoginLog::getLoginTime));
        return Result.success(new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @DeleteMapping("/clean")
    @Log(title = "登录日志", businessType = 3)
    @PreAuthorize("hasAuthority('monitor:loginlog:remove')")
    public Result<Void> clean() {
        loginLogMapper.delete(null);
        return Result.success();
    }
}
