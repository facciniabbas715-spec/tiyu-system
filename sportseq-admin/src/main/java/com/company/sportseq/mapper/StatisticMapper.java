package com.company.sportseq.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 统计报表聚合查询 Mapper（SQL 见 resources/mapper/StatisticMapper.xml）。
 */
public interface StatisticMapper {

    Map<String, Object> summary();

    List<Map<String, Object>> categoryStock();

    List<Map<String, Object>> warehouseStock();

    List<Map<String, Object>> borrowTrendBorrow(@Param("startDate") String startDate,
                                                @Param("endDate") String endDate);

    List<Map<String, Object>> borrowTrendReturn(@Param("startDate") String startDate,
                                                @Param("endDate") String endDate);

    List<Map<String, Object>> equipmentUsageTop(@Param("limit") int limit,
                                                @Param("startDate") String startDate,
                                                @Param("endDate") String endDate);

    List<Map<String, Object>> deptBorrowStats(@Param("startDate") String startDate,
                                              @Param("endDate") String endDate);

    List<Map<String, Object>> overdueItems(@Param("startDate") String startDate,
                                           @Param("endDate") String endDate);
}
