package com.company.sportseq.controller;

import com.alibaba.excel.EasyExcel;
import com.company.sportseq.annotation.Log;
import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.common.result.Result;
import com.company.sportseq.dto.EquipmentDTO;
import com.company.sportseq.service.EquipmentService;
import com.company.sportseq.vo.EquipmentExportVO;
import com.company.sportseq.vo.EquipmentVO;
import com.company.sportseq.vo.ImportResultVO;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

    @GetMapping("/page")
    @PreAuthorize("hasAuthority('equipment:list')")
    public Result<PageResult<EquipmentVO>> page(@RequestParam(defaultValue = "1") long current,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String equipmentName,
                                                @RequestParam(required = false) String equipmentCode,
                                                @RequestParam(required = false) Long categoryId,
                                                @RequestParam(required = false) Integer status) {
        return Result.success(equipmentService.page(current, size, equipmentName, equipmentCode, categoryId, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('equipment:list')")
    public Result<EquipmentVO> detail(@PathVariable Long id) {
        return Result.success(equipmentService.detail(id));
    }

    @PostMapping
    @Log(title = "器材档案管理", businessType = 1)
    @PreAuthorize("hasAuthority('equipment:add')")
    public Result<Void> add(@Valid @RequestBody EquipmentDTO dto) {
        equipmentService.add(dto);
        return Result.success();
    }

    @PutMapping
    @Log(title = "器材档案管理", businessType = 2)
    @PreAuthorize("hasAuthority('equipment:edit')")
    public Result<Void> update(@Valid @RequestBody EquipmentDTO dto) {
        equipmentService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Log(title = "器材档案管理", businessType = 3)
    @PreAuthorize("hasAuthority('equipment:remove')")
    public Result<Void> remove(@PathVariable Long id) {
        equipmentService.remove(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    @Log(title = "器材状态", businessType = 2)
    @PreAuthorize("hasAuthority('equipment:edit')")
    public Result<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        equipmentService.changeStatus(id, status);
        return Result.success();
    }

    @PostMapping("/import")
    @Log(title = "器材导入", businessType = 6)
    @PreAuthorize("hasAuthority('equipment:import')")
    public Result<ImportResultVO> importExcel(@RequestParam("file") MultipartFile file) {
        return Result.success(equipmentService.importExcel(file));
    }

    @GetMapping("/export")
    @Log(title = "器材导出", businessType = 5)
    @PreAuthorize("hasAuthority('equipment:export')")
    public void export(@RequestParam(required = false) String equipmentName,
                       @RequestParam(required = false) String equipmentCode,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) Integer status,
                       HttpServletResponse response) throws IOException {
        List<EquipmentVO> list = equipmentService.exportList(equipmentName, equipmentCode, categoryId, status);
        List<EquipmentExportVO> rows = list.stream()
                .map(e -> new EquipmentExportVO(e.equipmentCode(), e.equipmentName(), e.categoryName(),
                        e.brand(), e.model(), e.spec(), e.unit(), e.purchasePrice(), e.safeStock(),
                        e.status() == 1 ? "正常" : "停用", e.createTime()))
                .toList();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("器材列表", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
        EasyExcel.write(response.getOutputStream(), EquipmentExportVO.class)
                .sheet("器材列表")
                .doWrite(rows);
    }
}
