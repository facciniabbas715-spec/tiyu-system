package com.company.sportseq.controller;

import com.company.sportseq.annotation.Log;
import com.company.sportseq.common.result.Result;
import com.company.sportseq.dto.WarehouseDTO;
import com.company.sportseq.entity.Warehouse;
import com.company.sportseq.service.WarehouseService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/equipment/warehouse")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('equipment:warehouse:list')")
    public Result<List<Warehouse>> list() {
        return Result.success(warehouseService.list());
    }

    @PostMapping
    @Log(title = "仓库管理", businessType = 1)
    @PreAuthorize("hasAuthority('equipment:warehouse:add')")
    public Result<Void> add(@Valid @RequestBody WarehouseDTO dto) {
        warehouseService.add(dto);
        return Result.success();
    }

    @PutMapping
    @Log(title = "仓库管理", businessType = 2)
    @PreAuthorize("hasAuthority('equipment:warehouse:edit')")
    public Result<Void> update(@Valid @RequestBody WarehouseDTO dto) {
        warehouseService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Log(title = "仓库管理", businessType = 3)
    @PreAuthorize("hasAuthority('equipment:warehouse:remove')")
    public Result<Void> remove(@PathVariable Long id) {
        warehouseService.remove(id);
        return Result.success();
    }
}
