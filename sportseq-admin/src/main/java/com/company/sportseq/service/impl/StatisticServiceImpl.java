package com.company.sportseq.service.impl;

import cn.hutool.core.util.StrUtil;
import com.company.sportseq.common.exception.BizException;
import com.company.sportseq.common.exception.ErrorCode;
import com.company.sportseq.mapper.StatisticMapper;
import com.company.sportseq.service.StatisticService;
import com.company.sportseq.vo.BorrowTrendVO;
import com.company.sportseq.vo.CategoryStockVO;
import com.company.sportseq.vo.DashboardSummaryVO;
import com.company.sportseq.vo.DeptBorrowVO;
import com.company.sportseq.vo.EquipmentUsageVO;
import com.company.sportseq.vo.OverdueItemVO;
import com.company.sportseq.vo.OverdueStatVO;
import com.company.sportseq.vo.WarehouseStockVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 统计报表服务实现：MyBatis XML 聚合查询 + Java 补零/换算。
 */
@Service
@RequiredArgsConstructor
public class StatisticServiceImpl implements StatisticService {

    private static final int USAGE_TOP_N = 10;
    private static final int TREND_MONTHS = 12;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final StatisticMapper statisticMapper;

    @Override
    public DashboardSummaryVO summary() {
        Map<String, Object> row = statisticMapper.summary();
        return new DashboardSummaryVO(
                longVal(row.get("todayBorrowCount")),
                longVal(row.get("todayReturnCount")),
                longVal(row.get("todayStockInCount")),
                longVal(row.get("warningStockCount")),
                longVal(row.get("pendingScrapCount")),
                longVal(row.get("equipmentTotal")),
                decimalVal(row.get("stockTotalValue"))
        );
    }

    @Override
    public List<CategoryStockVO> categoryStock() {
        return statisticMapper.categoryStock().stream()
                .map(row -> new CategoryStockVO(
                        StrUtil.toString(row.get("categoryName")),
                        longVal(row.get("quantity"))))
                .toList();
    }

    @Override
    public List<WarehouseStockVO> warehouseStock() {
        return statisticMapper.warehouseStock().stream()
                .map(row -> new WarehouseStockVO(
                        StrUtil.toString(row.get("warehouseName")),
                        longVal(row.get("quantity"))))
                .toList();
    }

    @Override
    public List<BorrowTrendVO> borrowTrend(String startDate, String endDate) {
        Range range = resolveRange(startDate, endDate);
        List<YearMonth> months = monthList(range.start(), range.end());

        Map<String, Long> borrowMap = toMonthQuantityMap(
                statisticMapper.borrowTrendBorrow(dateStr(range.start()), dateStr(range.end())));
        Map<String, Long> returnMap = toMonthQuantityMap(
                statisticMapper.borrowTrendReturn(dateStr(range.start()), dateStr(range.end())));

        List<BorrowTrendVO> result = new ArrayList<>(months.size());
        for (YearMonth month : months) {
            String key = month.format(MONTH_FMT);
            result.add(new BorrowTrendVO(
                    key,
                    borrowMap.getOrDefault(key, 0L),
                    returnMap.getOrDefault(key, 0L)));
        }
        return result;
    }

    @Override
    public List<EquipmentUsageVO> equipmentUsageTop(String startDate, String endDate) {
        Range range = resolveRange(startDate, endDate);
        return statisticMapper.equipmentUsageTop(USAGE_TOP_N, dateStr(range.start()), dateStr(range.end())).stream()
                .map(row -> new EquipmentUsageVO(
                        StrUtil.toString(row.get("equipmentCode")),
                        StrUtil.toString(row.get("equipmentName")),
                        StrUtil.toString(row.get("categoryName")),
                        longVal(row.get("borrowCount"))))
                .toList();
    }

    @Override
    public List<DeptBorrowVO> deptBorrowStats(String startDate, String endDate) {
        Range range = resolveRange(startDate, endDate);
        return statisticMapper.deptBorrowStats(dateStr(range.start()), dateStr(range.end())).stream()
                .map(row -> new DeptBorrowVO(
                        StrUtil.toString(row.get("deptName")),
                        longVal(row.get("borrowCount")),
                        longVal(row.get("borrowQuantity")),
                        longVal(row.get("outstandingQuantity"))))
                .toList();
    }

    @Override
    public OverdueStatVO overdueStats(String startDate, String endDate) {
        List<OverdueItemVO> items = overdueItems(startDate, endDate);
        if (items.isEmpty()) {
            return new OverdueStatVO(0, 0, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        long orderCount = items.stream().map(OverdueItemVO::returnOrderNo).distinct().count();
        long itemCount = items.size();
        BigDecimal totalDays = items.stream()
                .map(OverdueItemVO::overdueDays)
                .filter(days -> days != null)
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgDays = totalDays.divide(BigDecimal.valueOf(itemCount), 1, RoundingMode.HALF_UP);
        BigDecimal penaltyTotal = items.stream()
                .map(OverdueItemVO::penaltyAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OverdueStatVO(orderCount, itemCount, avgDays, penaltyTotal);
    }

    @Override
    public List<OverdueItemVO> overdueItems(String startDate, String endDate) {
        Range range = resolveRange(startDate, endDate);
        return statisticMapper.overdueItems(dateStr(range.start()), dateStr(range.end())).stream()
                .map(row -> new OverdueItemVO(
                        StrUtil.toString(row.get("returnOrderNo")),
                        StrUtil.toString(row.get("borrowOrderNo")),
                        StrUtil.toString(row.get("equipmentCode")),
                        StrUtil.toString(row.get("equipmentName")),
                        intVal(row.get("quantity")),
                        intVal(row.get("overdueDays")),
                        decimalVal(row.get("penaltyAmount")),
                        toLocalDateTime(row.get("confirmTime"))))
                .toList();
    }

    /**
     * 解析时间范围：缺省时为近 12 个自然月；两者都传时按闭区间校验。
     */
    private Range resolveRange(String startDate, String endDate) {
        String start = StrUtil.trimToNull(startDate);
        String end = StrUtil.trimToNull(endDate);
        if (start == null && end == null) {
            YearMonth endMonth = YearMonth.now();
            return new Range(endMonth.minusMonths(TREND_MONTHS - 1).atDay(1), endMonth.atEndOfMonth());
        }
        LocalDate endDateValue = parseDate(end, "结束日期");
        LocalDate startDateValue = parseDate(start, "开始日期");
        if (start == null) {
            startDateValue = endDateValue.minusMonths(TREND_MONTHS - 1);
        } else if (end == null) {
            endDateValue = LocalDate.now();
        }
        if (startDateValue.isAfter(endDateValue)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "开始日期不能晚于结束日期");
        }
        return new Range(startDateValue, endDateValue);
    }

    private LocalDate parseDate(String value, String label) {
        if (value == null) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(value, DATE_FMT);
        } catch (DateTimeParseException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, label + "格式不正确，应为 yyyy-MM-dd");
        }
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof java.util.Date date) {
            return new Timestamp(date.getTime()).toLocalDateTime();
        }
        return LocalDateTime.parse(value.toString().replace(' ', 'T'));
    }

    private List<YearMonth> monthList(LocalDate start, LocalDate end) {
        YearMonth from = YearMonth.from(start);
        YearMonth to = YearMonth.from(end);
        List<YearMonth> months = new ArrayList<>();
        for (YearMonth month = from; !month.isAfter(to); month = month.plusMonths(1)) {
            months.add(month);
        }
        return months;
    }

    private String dateStr(LocalDate date) {
        return date == null ? null : date.format(DATE_FMT);
    }

    private Map<String, Long> toMonthQuantityMap(List<Map<String, Object>> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            Object month = row.get("month");
            if (month != null) {
                map.put(month.toString(), longVal(row.get("quantity")));
            }
        }
        return map;
    }

    private long longVal(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private int intVal(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private BigDecimal decimalVal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(value.toString());
    }

    private record Range(LocalDate start, LocalDate end) {
    }
}
