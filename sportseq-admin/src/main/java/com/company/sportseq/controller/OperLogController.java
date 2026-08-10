package com.company.sportseq.controller;

import com.company.sportseq.annotation.Log;
import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.common.result.Result;
import com.company.sportseq.entity.SysOperLog;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.sportseq.mapper.SysOperLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/monitor/operlog")
@RequiredArgsConstructor
public class OperLogController {

    private final SysOperLogMapper operLogMapper;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('monitor:operlog:list')")
    public Result<PageResult<SysOperLog>> page(@RequestParam(defaultValue = "1") long current,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String title,
                                               @RequestParam(required = false) String operName,
                                               @RequestParam(required = false) Integer status) {
        Page<SysOperLog> page = operLogMapper.selectPage(new Page<>(current, size),
                Wrappers.<SysOperLog>lambdaQuery()
                        .like(title != null && !title.isBlank(), SysOperLog::getTitle, title)
                        .like(operName != null && !operName.isBlank(), SysOperLog::getOperName, operName)
                        .eq(status != null, SysOperLog::getStatus, status)
                        .orderByDesc(SysOperLog::getOperTime));
        return Result.success(new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @DeleteMapping("/{id}")
    @Log(title = "操作日志", businessType = 3)
    @PreAuthorize("hasAuthority('monitor:operlog:remove')")
    public Result<Void> remove(@PathVariable Long id) {
        operLogMapper.deleteById(id);
        return Result.success();
    }

    @DeleteMapping("/clean")
    @Log(title = "操作日志", businessType = 3)
    @PreAuthorize("hasAuthority('monitor:operlog:remove')")
    public Result<Void> clean() {
        operLogMapper.delete(null);
        return Result.success();
    }
}
