package com.company.sportseq.controller;

import com.company.sportseq.annotation.Log;
import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.common.result.Result;
import com.company.sportseq.dto.DictDataDTO;
import com.company.sportseq.dto.DictTypeDTO;
import com.company.sportseq.entity.SysDictData;
import com.company.sportseq.entity.SysDictType;
import com.company.sportseq.service.SysDictService;
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
@RequestMapping("/api/system/dict")
@RequiredArgsConstructor
public class SysDictController {

    private final SysDictService dictService;

    @GetMapping("/type/page")
    @PreAuthorize("hasAuthority('system:dict:list')")
    public Result<PageResult<SysDictType>> typePage(@RequestParam(defaultValue = "1") long current,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) String dictName,
                                                    @RequestParam(required = false) String dictType) {
        return Result.success(dictService.typePage(current, size, dictName, dictType));
    }

    @PostMapping("/type")
    @Log(title = "字典类型管理", businessType = 1)
    @PreAuthorize("hasAuthority('system:dict:add')")
    public Result<Void> addType(@Valid @RequestBody DictTypeDTO dto) {
        dictService.addType(dto);
        return Result.success();
    }

    @PutMapping("/type")
    @Log(title = "字典类型管理", businessType = 2)
    @PreAuthorize("hasAuthority('system:dict:edit')")
    public Result<Void> updateType(@Valid @RequestBody DictTypeDTO dto) {
        dictService.updateType(dto);
        return Result.success();
    }

    @DeleteMapping("/type/{id}")
    @Log(title = "字典类型管理", businessType = 3)
    @PreAuthorize("hasAuthority('system:dict:remove')")
    public Result<Void> removeType(@PathVariable Long id) {
        dictService.removeType(id);
        return Result.success();
    }

    @GetMapping("/data/page")
    @PreAuthorize("hasAuthority('system:dict:list')")
    public Result<PageResult<SysDictData>> dataPage(@RequestParam(defaultValue = "1") long current,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) String dictType) {
        return Result.success(dictService.dataPage(current, size, dictType));
    }

    @GetMapping("/data/type/{dictType}")
    public Result<List<SysDictData>> listByType(@PathVariable String dictType) {
        return Result.success(dictService.listByType(dictType));
    }

    @PostMapping("/data")
    @Log(title = "字典数据管理", businessType = 1)
    @PreAuthorize("hasAuthority('system:dict:add')")
    public Result<Void> addData(@Valid @RequestBody DictDataDTO dto) {
        dictService.addData(dto);
        return Result.success();
    }

    @PutMapping("/data")
    @Log(title = "字典数据管理", businessType = 2)
    @PreAuthorize("hasAuthority('system:dict:edit')")
    public Result<Void> updateData(@Valid @RequestBody DictDataDTO dto) {
        dictService.updateData(dto);
        return Result.success();
    }

    @DeleteMapping("/data/{id}")
    @Log(title = "字典数据管理", businessType = 3)
    @PreAuthorize("hasAuthority('system:dict:remove')")
    public Result<Void> removeData(@PathVariable Long id) {
        dictService.removeData(id);
        return Result.success();
    }
}
