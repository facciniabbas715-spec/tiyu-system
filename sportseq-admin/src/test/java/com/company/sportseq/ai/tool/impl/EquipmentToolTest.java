package com.company.sportseq.ai.tool.impl;

import com.company.sportseq.ai.tool.AiToolOutcome;
import com.company.sportseq.common.exception.BizException;
import com.company.sportseq.common.exception.ErrorCode;
import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.service.EquipmentService;
import com.company.sportseq.vo.EquipmentVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 器材工具单元测试：详情/搜索映射真实 Service 数据，异常转为友好文案。
 */
class EquipmentToolTest {

    @Test
    void detail_shouldMapEquipmentVo() {
        EquipmentService service = mock(EquipmentService.class);
        EquipmentVO vo = new EquipmentVO(7L, "BALL-2026-000001", "篮球", 3L, "球类",
                "斯伯丁", "74-569Y", "7号", "个", new BigDecimal("199.00"),
                10, 15, null, 1, "比赛用球", LocalDateTime.now());
        when(service.detail(7L)).thenReturn(vo);
        EquipmentTool tool = new EquipmentTool(service);

        AiToolOutcome outcome = tool.getEquipmentDetail(7L);

        assertTrue(outcome.ok());
        EquipmentTool.EquipmentDetail detail = (EquipmentTool.EquipmentDetail) outcome.data();
        assertEquals("篮球", detail.equipmentName());
        assertEquals("球类", detail.categoryName());
        assertEquals(15, detail.maxBorrowDays());
        assertEquals(1, detail.status());
    }

    @Test
    void detail_shouldReturnMessageWhenNotFound() {
        EquipmentService service = mock(EquipmentService.class);
        when(service.detail(999L)).thenThrow(new BizException(ErrorCode.PARAM_ERROR, "器材不存在"));
        EquipmentTool tool = new EquipmentTool(service);

        AiToolOutcome outcome = tool.getEquipmentDetail(999L);

        assertFalse(outcome.ok());
        assertEquals("器材不存在", outcome.message());
        assertNull(outcome.data());
    }

    @Test
    void search_shouldReturnBriefList() {
        EquipmentService service = mock(EquipmentService.class);
        List<EquipmentVO> rows = List.of(
                new EquipmentVO(7L, "BALL-2026-000001", "篮球", 3L, "球类", null, null, null,
                        "个", null, 10, 15, null, 1, null, null),
                new EquipmentVO(8L, "RKT-2026-000002", "羽毛球拍", 3L, "球类", null, null, null,
                        "支", null, 5, 10, null, 1, null, null));
        when(service.page(1, 20, "球", null, null, null)).thenReturn(new PageResult<>(2, 1, 20, rows));
        EquipmentTool tool = new EquipmentTool(service);

        AiToolOutcome outcome = tool.searchEquipment("球");

        assertTrue(outcome.ok());
        List<EquipmentTool.EquipmentBrief> briefs = cast(outcome.data());
        assertEquals(2, briefs.size());
        assertEquals("篮球", briefs.get(0).equipmentName());
        assertEquals(15, briefs.get(0).maxBorrowDays());
    }

    @Test
    void search_shouldRequireKeyword() {
        EquipmentService service = mock(EquipmentService.class);
        EquipmentTool tool = new EquipmentTool(service);

        AiToolOutcome outcome = tool.searchEquipment(" ");

        assertFalse(outcome.ok());
        assertTrue(outcome.message().contains("名称关键字"));
    }

    @SuppressWarnings("unchecked")
    private List<EquipmentTool.EquipmentBrief> cast(Object data) {
        return (List<EquipmentTool.EquipmentBrief>) data;
    }
}
