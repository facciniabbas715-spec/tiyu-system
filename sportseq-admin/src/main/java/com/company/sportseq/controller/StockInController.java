package com.company.sportseq.controller;

import com.company.sportseq.annotation.Log;
import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.common.result.Result;
import com.company.sportseq.dto.StockInAuditDTO;
import com.company.sportseq.dto.StockInOrderDTO;
import com.company.sportseq.service.StockInService;
import com.company.sportseq.vo.StockInVO;
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
@RequestMapping("/api/stock/in")
@RequiredArgsConstructor
public class StockInController {

    private final StockInService stockInService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('stock:in:list')")
    public Result<PageResult<StockInVO>> page(@RequestParam(defaultValue = "1") long current,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String orderNo,
                                              @RequestParam(required = false) Integer status) {
        return Result.success(stockInService.page(current, size, orderNo, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('stock:in:list')")
    public Result<StockInVO> detail(@PathVariable Long id) {
        return Result.success(stockInService.detail(id));
    }

    @PostMapping
    @Log(title = "入库单管理", businessType = 1)
    @PreAuthorize("hasAuthority('stock:in:add')")
    public Result<Void> create(@Valid @RequestBody StockInOrderDTO dto) {
        stockInService.create(dto);
        return Result.success();
    }

    @PutMapping("/{id}/submit")
    @Log(title = "入库单管理", businessType = 2)
    @PreAuthorize("hasAuthority('stock:in:add')")
    public Result<Void> submit(@PathVariable Long id) {
        stockInService.submit(id);
        return Result.success();
    }

    @PutMapping("/audit")
    @Log(title = "入库单审核", businessType = 4)
    @PreAuthorize("hasAuthority('stock:in:audit')")
    public Result<Void> audit(@Valid @RequestBody StockInAuditDTO dto) {
        stockInService.audit(dto);
        return Result.success();
    }

    @PutMapping("/{id}/receive")
    @Log(title = "入库验收", businessType = 2)
    @PreAuthorize("hasAuthority('stock:in:receive')")
    public Result<Void> receive(@PathVariable Long id) {
        stockInService.receive(id);
        return Result.success();
    }

    @PutMapping("/{id}/cancel")
    @Log(title = "入库单管理", businessType = 2)
    @PreAuthorize("hasAuthority('stock:in:add')")
    public Result<Void> cancel(@PathVariable Long id) {
        stockInService.cancel(id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Log(title = "入库单管理", businessType = 3)
    @PreAuthorize("hasAuthority('stock:in:remove')")
    public Result<Void> remove(@PathVariable Long id) {
        stockInService.remove(id);
        return Result.success();
    }
}
