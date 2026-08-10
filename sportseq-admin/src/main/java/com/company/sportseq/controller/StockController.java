package com.company.sportseq.controller;

import com.company.sportseq.annotation.Log;
import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.common.result.Result;
import com.company.sportseq.dto.StockAdjustDTO;
import com.company.sportseq.service.StockService;
import com.company.sportseq.vo.StockRecordVO;
import com.company.sportseq.vo.StockVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('stock:list')")
    public Result<PageResult<StockVO>> page(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String equipmentName,
                                            @RequestParam(required = false) String equipmentCode,
                                            @RequestParam(required = false) Long warehouseId,
                                            @RequestParam(required = false) Long categoryId,
                                            @RequestParam(required = false) Boolean warningOnly) {
        return Result.success(stockService.page(current, size, equipmentName, equipmentCode,
                warehouseId, categoryId, warningOnly));
    }

    @GetMapping("/record/page")
    @PreAuthorize("hasAuthority('stock:record:list')")
    public Result<PageResult<StockRecordVO>> recordPage(@RequestParam(defaultValue = "1") long current,
                                                        @RequestParam(defaultValue = "10") long size,
                                                        @RequestParam(required = false) Long equipmentId,
                                                        @RequestParam(required = false) Long warehouseId,
                                                        @RequestParam(required = false) Integer changeType) {
        return Result.success(stockService.recordPage(current, size, equipmentId, warehouseId, changeType));
    }

    @GetMapping("/warning")
    @PreAuthorize("hasAuthority('stock:list')")
    public Result<List<StockVO>> warnings() {
        return Result.success(stockService.warnings());
    }

    @PutMapping("/adjust")
    @Log(title = "库存调整", businessType = 2)
    @PreAuthorize("hasAuthority('stock:adjust')")
    public Result<Void> adjust(@Valid @RequestBody StockAdjustDTO dto) {
        stockService.adjust(dto);
        return Result.success();
    }
}
