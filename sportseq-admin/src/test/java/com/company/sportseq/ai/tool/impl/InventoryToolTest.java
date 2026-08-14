package com.company.sportseq.ai.tool.impl;

import com.company.sportseq.ai.tool.AiToolOutcome;
import com.company.sportseq.common.exception.BizException;
import com.company.sportseq.common.exception.ErrorCode;
import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.service.StockService;
import com.company.sportseq.vo.StockVO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 库存工具单元测试：跨仓库聚合真实库存行，可用数量=在库-锁定，无数据/失败返回固定文案。
 */
class InventoryToolTest {

    @Test
    void stock_shouldAggregateAcrossWarehousesAndComputeAvailable() {
        StockService service = mock(StockService.class);
        List<StockVO> rows = List.of(
                new StockVO(7L, "BALL-2026-000001", "篮球", "球类", "个", 1L, "一号仓库", 10, 2, 5, false),
                new StockVO(7L, "BALL-2026-000001", "篮球", "球类", "个", 2L, "二号仓库", 5, 1, 5, false));
        when(service.page(1, 50, "篮球", null, null, null, false)).thenReturn(new PageResult<>(2, 1, 50, rows));
        InventoryTool tool = new InventoryTool(service);

        AiToolOutcome outcome = tool.getEquipmentStock("篮球");

        assertTrue(outcome.ok());
        List<InventoryTool.EquipmentStockSummary> summaries = cast(outcome.data());
        assertEquals(1, summaries.size());
        InventoryTool.EquipmentStockSummary summary = summaries.get(0);
        assertEquals("篮球", summary.equipmentName());
        assertEquals(15, summary.totalQuantity());
        assertEquals(3, summary.totalLockedQuantity());
        assertEquals(12, summary.availableQuantity());
        assertEquals(2, summary.warehouses().size());
    }

    @Test
    void stock_shouldKeepDifferentEquipmentSeparate() {
        StockService service = mock(StockService.class);
        List<StockVO> rows = List.of(
                new StockVO(7L, "BALL-2026-000001", "篮球", "球类", "个", 1L, "一号仓库", 10, 0, 5, false),
                new StockVO(8L, "RKT-2026-000002", "羽毛球拍", "球类", "支", 1L, "一号仓库", 4, 1, 2, false));
        when(service.page(1, 50, "球", null, null, null, false)).thenReturn(new PageResult<>(2, 1, 50, rows));
        InventoryTool tool = new InventoryTool(service);

        AiToolOutcome outcome = tool.getEquipmentStock("球");

        assertTrue(outcome.ok());
        List<InventoryTool.EquipmentStockSummary> summaries = cast(outcome.data());
        assertEquals(2, summaries.size());
    }

    @Test
    void stock_shouldRequireEquipmentName() {
        StockService service = mock(StockService.class);
        InventoryTool tool = new InventoryTool(service);

        AiToolOutcome outcome = tool.getEquipmentStock(" ");

        assertFalse(outcome.ok());
        assertTrue(outcome.message().contains("器材名称"));
        verify(service, never()).page(anyLong(), anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    void stock_shouldReturnMessageWhenNoStockRow() {
        StockService service = mock(StockService.class);
        when(service.page(1, 50, "篮球", null, null, null, false))
                .thenReturn(new PageResult<>(0, 1, 50, List.of()));
        InventoryTool tool = new InventoryTool(service);

        AiToolOutcome outcome = tool.getEquipmentStock("篮球");

        assertFalse(outcome.ok());
        assertTrue(outcome.message().contains("篮球"));
        assertTrue(outcome.message().contains("未查询到"));
    }

    @Test
    void stock_shouldReturnFriendlyErrorOnFailure() {
        StockService service = mock(StockService.class);
        when(service.page(1, 50, "篮球", null, null, null, false))
                .thenThrow(new BizException(ErrorCode.SYSTEM_ERROR, "库存查询失败"));
        InventoryTool tool = new InventoryTool(service);

        AiToolOutcome outcome = tool.getEquipmentStock("篮球");

        assertFalse(outcome.ok());
        assertTrue(outcome.message().contains("稍后重试"));
    }

    @SuppressWarnings("unchecked")
    private List<InventoryTool.EquipmentStockSummary> cast(Object data) {
        return (List<InventoryTool.EquipmentStockSummary>) data;
    }
}
