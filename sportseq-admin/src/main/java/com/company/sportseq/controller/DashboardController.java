package com.company.sportseq.controller;

import com.company.sportseq.common.result.Result;
import com.company.sportseq.service.StatisticService;
import com.company.sportseq.vo.DashboardSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据仪表盘接口。
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final StatisticService statisticService;

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public Result<DashboardSummaryVO> summary() {
        return Result.success(statisticService.summary());
    }
}
