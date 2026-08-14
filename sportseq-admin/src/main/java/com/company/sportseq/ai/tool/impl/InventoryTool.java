package com.company.sportseq.ai.tool.impl;

import com.company.sportseq.ai.tool.AiTool;
import com.company.sportseq.ai.tool.AiToolOutcome;
import com.company.sportseq.ai.tool.AiToolPermission;
import com.company.sportseq.service.StockService;
import com.company.sportseq.vo.StockVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 库存工具：按器材名称查询真实库存，数据来自 {@link StockService} 只读分页接口。
 *
 * <p>跨仓库聚合，可用数量 = 在库数量 - 锁定数量；无库存行时返回固定文案，
 * 不允许模型自行估计数量。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@AiToolPermission("stock:list")
public class InventoryTool implements AiTool {

    private final StockService stockService;

    @Tool(description = "查询指定器材名称的真实库存：返回各仓库的在库数量、锁定数量与可用数量汇总（可用=在库-锁定），数据来自业务数据库")
    public AiToolOutcome getEquipmentStock(
            @ToolParam(description = "器材名称，例如：篮球") String equipmentName) {
        if (equipmentName == null || equipmentName.isBlank()) {
            return AiToolOutcome.failure("请提供要查询库存的器材名称，例如：篮球。");
        }
        String keyword = equipmentName.trim();
        try {
            List<StockVO> rows = stockService.page(1, 50, keyword, null, null, null, false)
                    .records();
            if (rows.isEmpty()) {
                return AiToolOutcome.failure("未查询到「" + keyword + "」的库存记录。");
            }
            Map<Long, List<StockVO>> grouped = rows.stream().collect(Collectors.groupingBy(
                    StockVO::equipmentId, LinkedHashMap::new, Collectors.toList()));
            List<EquipmentStockSummary> summaries = grouped.values().stream()
                    .map(this::toSummary)
                    .toList();
            return AiToolOutcome.success(summaries);
        } catch (Exception e) {
            log.warn("库存工具调用失败，equipmentName={}", keyword, e);
            return AiToolOutcome.failure("库存查询失败，请稍后重试。");
        }
    }

    private EquipmentStockSummary toSummary(List<StockVO> rows) {
        StockVO first = rows.get(0);
        int totalQuantity = rows.stream().mapToInt(row -> quantityOf(row.quantity())).sum();
        int totalLocked = rows.stream().mapToInt(row -> quantityOf(row.lockedQuantity())).sum();
        List<WarehouseStock> warehouses = rows.stream()
                .map(row -> new WarehouseStock(row.warehouseName(),
                        quantityOf(row.quantity()), quantityOf(row.lockedQuantity()),
                        quantityOf(row.quantity()) - quantityOf(row.lockedQuantity())))
                .toList();
        return new EquipmentStockSummary(first.equipmentId(), first.equipmentCode(),
                first.equipmentName(), first.categoryName(), first.unit(),
                totalQuantity, totalLocked, totalQuantity - totalLocked, warehouses);
    }

    private int quantityOf(Integer quantity) {
        return quantity == null ? 0 : quantity;
    }

    /**
     * 单件器材的库存汇总。
     */
    public record EquipmentStockSummary(Long equipmentId, String equipmentCode,
                                        String equipmentName, String categoryName, String unit,
                                        Integer totalQuantity, Integer totalLockedQuantity,
                                        Integer availableQuantity, List<WarehouseStock> warehouses) {
    }

    /**
     * 单个仓库的库存明细。
     */
    public record WarehouseStock(String warehouseName, Integer quantity,
                                 Integer lockedQuantity, Integer availableQuantity) {
    }
}
