package com.company.sportseq.controller;

import com.alibaba.excel.EasyExcel;
import com.company.sportseq.annotation.Log;
import com.company.sportseq.common.result.Result;
import com.company.sportseq.service.StatisticService;
import com.company.sportseq.vo.BorrowTrendExportVO;
import com.company.sportseq.vo.BorrowTrendVO;
import com.company.sportseq.vo.CategoryStockExportVO;
import com.company.sportseq.vo.CategoryStockVO;
import com.company.sportseq.vo.DeptBorrowExportVO;
import com.company.sportseq.vo.DeptBorrowVO;
import com.company.sportseq.vo.EquipmentUsageExportVO;
import com.company.sportseq.vo.EquipmentUsageVO;
import com.company.sportseq.vo.OverdueExportVO;
import com.company.sportseq.vo.OverdueItemVO;
import com.company.sportseq.vo.OverdueStatVO;
import com.company.sportseq.vo.WarehouseStockExportVO;
import com.company.sportseq.vo.WarehouseStockVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 统计报表接口：ECharts 报表数据与 Excel 导出。
 */
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticController {

    private final StatisticService statisticService;

    @GetMapping("/category-stock")
    @PreAuthorize("hasAuthority('statistics:view')")
    public Result<List<CategoryStockVO>> categoryStock() {
        return Result.success(statisticService.categoryStock());
    }

    @GetMapping("/warehouse-stock")
    @PreAuthorize("hasAuthority('statistics:view')")
    public Result<List<WarehouseStockVO>> warehouseStock() {
        return Result.success(statisticService.warehouseStock());
    }

    @GetMapping("/borrow-trend")
    @PreAuthorize("hasAuthority('statistics:view')")
    public Result<List<BorrowTrendVO>> borrowTrend(@RequestParam(required = false) String startDate,
                                                   @RequestParam(required = false) String endDate) {
        return Result.success(statisticService.borrowTrend(startDate, endDate));
    }

    @GetMapping("/equipment-usage")
    @PreAuthorize("hasAuthority('statistics:view')")
    public Result<List<EquipmentUsageVO>> equipmentUsage(@RequestParam(required = false) String startDate,
                                                         @RequestParam(required = false) String endDate) {
        return Result.success(statisticService.equipmentUsageTop(startDate, endDate));
    }

    @GetMapping("/dept-borrow")
    @PreAuthorize("hasAuthority('statistics:view')")
    public Result<List<DeptBorrowVO>> deptBorrow(@RequestParam(required = false) String startDate,
                                                 @RequestParam(required = false) String endDate) {
        return Result.success(statisticService.deptBorrowStats(startDate, endDate));
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAuthority('statistics:view')")
    public Result<OverdueStatVO> overdueStats(@RequestParam(required = false) String startDate,
                                              @RequestParam(required = false) String endDate) {
        return Result.success(statisticService.overdueStats(startDate, endDate));
    }

    @GetMapping("/overdue/items")
    @PreAuthorize("hasAuthority('statistics:view')")
    public Result<List<OverdueItemVO>> overdueItems(@RequestParam(required = false) String startDate,
                                                    @RequestParam(required = false) String endDate) {
        return Result.success(statisticService.overdueItems(startDate, endDate));
    }

    /**
     * 报表 Excel 导出。
     *
     * @param type 报表类型：category / warehouse / trend / usage / dept / overdue
     */
    @GetMapping("/export")
    @Log(title = "统计报表导出", businessType = 5)
    @PreAuthorize("hasAuthority('statistics:export')")
    public void export(@RequestParam String type,
                       @RequestParam(required = false) String startDate,
                       @RequestParam(required = false) String endDate,
                       HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode(sheetName(type), StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        switch (type) {
            case "category" -> writeCategory(response);
            case "warehouse" -> writeWarehouse(response);
            case "trend" -> writeTrend(response, startDate, endDate);
            case "usage" -> writeUsage(response, startDate, endDate);
            case "dept" -> writeDept(response, startDate, endDate);
            case "overdue" -> writeOverdue(response, startDate, endDate);
            default -> {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("不支持的报表类型: " + type);
            }
        }
    }

    private void writeCategory(HttpServletResponse response) throws IOException {
        List<CategoryStockExportVO> rows = statisticService.categoryStock().stream()
                .map(v -> new CategoryStockExportVO(v.categoryName(), v.quantity()))
                .toList();
        EasyExcel.write(response.getOutputStream(), CategoryStockExportVO.class)
                .sheet("库存分布")
                .doWrite(rows);
    }

    private void writeWarehouse(HttpServletResponse response) throws IOException {
        List<WarehouseStockExportVO> rows = statisticService.warehouseStock().stream()
                .map(v -> new WarehouseStockExportVO(v.warehouseName(), v.quantity()))
                .toList();
        EasyExcel.write(response.getOutputStream(), WarehouseStockExportVO.class)
                .sheet("仓库库存")
                .doWrite(rows);
    }

    private void writeTrend(HttpServletResponse response, String startDate, String endDate) throws IOException {
        List<BorrowTrendExportVO> rows = statisticService.borrowTrend(startDate, endDate).stream()
                .map(v -> new BorrowTrendExportVO(v.month(), v.borrowQuantity(), v.returnQuantity()))
                .toList();
        EasyExcel.write(response.getOutputStream(), BorrowTrendExportVO.class)
                .sheet("借用趋势")
                .doWrite(rows);
    }

    private void writeUsage(HttpServletResponse response, String startDate, String endDate) throws IOException {
        List<EquipmentUsageExportVO> rows = new ArrayList<>();
        List<EquipmentUsageVO> list = statisticService.equipmentUsageTop(startDate, endDate);
        for (int i = 0; i < list.size(); i++) {
            EquipmentUsageVO v = list.get(i);
            rows.add(new EquipmentUsageExportVO(i + 1, v.equipmentCode(), v.equipmentName(),
                    v.categoryName(), v.borrowCount()));
        }
        EasyExcel.write(response.getOutputStream(), EquipmentUsageExportVO.class)
                .sheet("使用率排行")
                .doWrite(rows);
    }

    private void writeDept(HttpServletResponse response, String startDate, String endDate) throws IOException {
        List<DeptBorrowExportVO> rows = statisticService.deptBorrowStats(startDate, endDate).stream()
                .map(v -> new DeptBorrowExportVO(v.deptName(), v.borrowCount(),
                        v.borrowQuantity(), v.outstandingQuantity()))
                .toList();
        EasyExcel.write(response.getOutputStream(), DeptBorrowExportVO.class)
                .sheet("部门统计")
                .doWrite(rows);
    }

    private void writeOverdue(HttpServletResponse response, String startDate, String endDate) throws IOException {
        List<OverdueExportVO> rows = statisticService.overdueItems(startDate, endDate).stream()
                .map(v -> new OverdueExportVO(v.returnOrderNo(), v.borrowOrderNo(),
                        v.equipmentCode(), v.equipmentName(), v.quantity(),
                        v.overdueDays(), v.penaltyAmount(), v.confirmTime()))
                .toList();
        EasyExcel.write(response.getOutputStream(), OverdueExportVO.class)
                .sheet("逾期明细")
                .doWrite(rows);
    }

    private String sheetName(String type) {
        return switch (type) {
            case "category" -> "库存分布";
            case "warehouse" -> "仓库库存";
            case "trend" -> "借用趋势";
            case "usage" -> "器材使用率";
            case "dept" -> "部门借用统计";
            case "overdue" -> "逾期统计";
            default -> "统计报表";
        };
    }
}
