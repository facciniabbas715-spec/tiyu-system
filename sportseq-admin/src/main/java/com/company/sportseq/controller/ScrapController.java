package com.company.sportseq.controller;

import com.company.sportseq.annotation.Log;
import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.common.result.Result;
import com.company.sportseq.dto.ScrapAuditDTO;
import com.company.sportseq.dto.ScrapDisposeDTO;
import com.company.sportseq.dto.ScrapOrderDTO;
import com.company.sportseq.service.ScrapService;
import com.company.sportseq.vo.ScrapOrderVO;
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
@RequestMapping("/api/scrap")
@RequiredArgsConstructor
public class ScrapController {

    private final ScrapService scrapService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('scrap:list')")
    public Result<PageResult<ScrapOrderVO>> page(@RequestParam(defaultValue = "1") long current,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String orderNo,
                                                 @RequestParam(required = false) Integer status) {
        return Result.success(scrapService.page(current, size, orderNo, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('scrap:list')")
    public Result<ScrapOrderVO> detail(@PathVariable Long id) {
        return Result.success(scrapService.detail(id));
    }

    @PostMapping
    @Log(title = "报废申请", businessType = 1)
    @PreAuthorize("hasAuthority('scrap:add')")
    public Result<Void> create(@Valid @RequestBody ScrapOrderDTO dto) {
        scrapService.create(dto);
        return Result.success();
    }

    @PutMapping("/audit")
    @Log(title = "报废审核", businessType = 4)
    @PreAuthorize("hasAuthority('scrap:audit')")
    public Result<Void> audit(@Valid @RequestBody ScrapAuditDTO dto) {
        scrapService.audit(dto);
        return Result.success();
    }

    @PutMapping("/dispose")
    @Log(title = "报废处置", businessType = 2)
    @PreAuthorize("hasAuthority('scrap:dispose')")
    public Result<Void> dispose(@Valid @RequestBody ScrapDisposeDTO dto) {
        scrapService.dispose(dto);
        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    @Log(title = "报废作废", businessType = 2)
    @PreAuthorize("hasAuthority('scrap:add')")
    public Result<Void> cancel(@PathVariable Long id) {
        scrapService.cancel(id);
        return Result.success();
    }
}
