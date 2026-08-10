package com.company.sportseq.controller;

import com.company.sportseq.annotation.Log;
import com.company.sportseq.common.result.Result;
import com.company.sportseq.dto.EquipmentCategoryDTO;
import com.company.sportseq.service.EquipmentCategoryService;
import com.company.sportseq.vo.EquipmentCategoryVO;
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
@RequestMapping("/api/equipment/category")
@RequiredArgsConstructor
public class EquipmentCategoryController {

    private final EquipmentCategoryService categoryService;

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('equipment:category:list')")
    public Result<List<EquipmentCategoryVO>> tree(@RequestParam(required = false) String categoryName) {
        return Result.success(categoryService.tree(categoryName));
    }

    @PostMapping
    @Log(title = "器材分类管理", businessType = 1)
    @PreAuthorize("hasAuthority('equipment:category:add')")
    public Result<Void> add(@Valid @RequestBody EquipmentCategoryDTO dto) {
        categoryService.add(dto);
        return Result.success();
    }

    @PutMapping
    @Log(title = "器材分类管理", businessType = 2)
    @PreAuthorize("hasAuthority('equipment:category:edit')")
    public Result<Void> update(@Valid @RequestBody EquipmentCategoryDTO dto) {
        categoryService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Log(title = "器材分类管理", businessType = 3)
    @PreAuthorize("hasAuthority('equipment:category:remove')")
    public Result<Void> remove(@PathVariable Long id) {
        categoryService.remove(id);
        return Result.success();
    }
}
