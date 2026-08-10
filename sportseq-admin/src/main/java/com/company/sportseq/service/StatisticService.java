package com.company.sportseq.service;

import com.company.sportseq.vo.BorrowTrendVO;
import com.company.sportseq.vo.CategoryStockVO;
import com.company.sportseq.vo.DashboardSummaryVO;
import com.company.sportseq.vo.DeptBorrowVO;
import com.company.sportseq.vo.EquipmentUsageVO;
import com.company.sportseq.vo.OverdueItemVO;
import com.company.sportseq.vo.OverdueStatVO;
import com.company.sportseq.vo.WarehouseStockVO;

import java.util.List;

/**
 * 统计报表服务：仪表盘汇总与各报表聚合数据。
 */
public interface StatisticService {

    DashboardSummaryVO summary();

    List<CategoryStockVO> categoryStock();

    List<WarehouseStockVO> warehouseStock();

    List<BorrowTrendVO> borrowTrend(String startDate, String endDate);

    List<EquipmentUsageVO> equipmentUsageTop(String startDate, String endDate);

    List<DeptBorrowVO> deptBorrowStats(String startDate, String endDate);

    OverdueStatVO overdueStats(String startDate, String endDate);

    List<OverdueItemVO> overdueItems(String startDate, String endDate);
}
