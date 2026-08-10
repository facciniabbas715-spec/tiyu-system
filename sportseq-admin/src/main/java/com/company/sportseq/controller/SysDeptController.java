package com.company.sportseq.controller;

import com.company.sportseq.common.result.Result;
import com.company.sportseq.dto.DeptDTO;
import com.company.sportseq.service.SysDeptService;
import com.company.sportseq.vo.DeptVO;
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
@RequestMapping("/api/system/dept")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptService deptService;

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:dept:list')")
    public Result<List<DeptVO>> tree(@RequestParam(required = false) String deptName) {
        return Result.success(deptService.tree(deptName));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:dept:add')")
    public Result<Void> add(@Valid @RequestBody DeptDTO dto) {
        deptService.add(dto);
        return Result.success();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system:dept:edit')")
    public Result<Void> update(@Valid @RequestBody DeptDTO dto) {
        deptService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept:remove')")
    public Result<Void> remove(@PathVariable Long id) {
        deptService.remove(id);
        return Result.success();
    }
}
