package com.company.sportseq.controller;

import com.company.sportseq.annotation.Log;
import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.common.result.Result;
import com.company.sportseq.dto.ConfigDTO;
import com.company.sportseq.entity.SysConfig;
import com.company.sportseq.service.SysConfigService;
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
@RequestMapping("/api/system/config")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService configService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:config:list')")
    public Result<PageResult<SysConfig>> page(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String configName,
                                              @RequestParam(required = false) String configKey) {
        return Result.success(configService.page(current, size, configName, configKey));
    }

    @GetMapping("/key/{configKey}")
    public Result<String> getValueByKey(@PathVariable String configKey) {
        return Result.success(configService.getValueByKey(configKey));
    }

    @PostMapping
    @Log(title = "参数管理", businessType = 1)
    @PreAuthorize("hasAuthority('system:config:add')")
    public Result<Void> add(@Valid @RequestBody ConfigDTO dto) {
        configService.add(dto);
        return Result.success();
    }

    @PutMapping
    @Log(title = "参数管理", businessType = 2)
    @PreAuthorize("hasAuthority('system:config:edit')")
    public Result<Void> update(@Valid @RequestBody ConfigDTO dto) {
        configService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Log(title = "参数管理", businessType = 3)
    @PreAuthorize("hasAuthority('system:config:remove')")
    public Result<Void> remove(@PathVariable Long id) {
        configService.remove(id);
        return Result.success();
    }
}
