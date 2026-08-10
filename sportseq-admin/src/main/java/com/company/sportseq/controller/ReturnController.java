package com.company.sportseq.controller;

import com.company.sportseq.annotation.Log;
import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.common.result.Result;
import com.company.sportseq.dto.ReturnOrderDTO;
import com.company.sportseq.service.ReturnService;
import com.company.sportseq.vo.ReturnOrderVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/return")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('return:list')")
    public Result<PageResult<ReturnOrderVO>> page(@RequestParam(defaultValue = "1") long current,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) String orderNo,
                                                  @RequestParam(required = false) Integer status) {
        return Result.success(returnService.page(current, size, orderNo, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('return:list')")
    public Result<ReturnOrderVO> detail(@PathVariable Long id) {
        return Result.success(returnService.detail(id));
    }

    @PostMapping
    @Log(title = "归还登记", businessType = 8)
    @PreAuthorize("hasAuthority('return:add')")
    public Result<Void> create(@Valid @RequestBody ReturnOrderDTO dto) {
        returnService.create(dto);
        return Result.success();
    }

    @PutMapping("/{id}/confirm")
    @Log(title = "归还确认", businessType = 4)
    @PreAuthorize("hasAuthority('return:confirm')")
    public Result<Void> confirm(@PathVariable Long id) {
        returnService.confirm(id);
        return Result.success();
    }

    @PutMapping("/{id}/reject")
    @Log(title = "归还驳回", businessType = 4)
    @PreAuthorize("hasAuthority('return:confirm')")
    public Result<Void> reject(@PathVariable Long id) {
        returnService.reject(id);
        return Result.success();
    }
}
