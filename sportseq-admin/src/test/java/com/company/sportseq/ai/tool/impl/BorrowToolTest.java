package com.company.sportseq.ai.tool.impl;

import com.company.sportseq.ai.tool.AiToolOutcome;
import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.security.LoginUser;
import com.company.sportseq.service.BorrowService;
import com.company.sportseq.service.SysConfigService;
import com.company.sportseq.vo.BorrowItemVO;
import com.company.sportseq.vo.BorrowOrderVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

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
 * 借用工具单元测试：借阅记录绑定当前登录用户（无 userId 参数）、状态映射与真实配置读取。
 */
class BorrowToolTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void records_shouldMapBorrowsOfCurrentUser() {
        BorrowService borrowService = mock(BorrowService.class);
        SysConfigService configService = mock(SysConfigService.class);
        login(42L);
        BorrowItemVO item = new BorrowItemVO(101L, 7L, "BALL-2026-000001", "篮球", "个", 1L,
                "一号仓库", 2, 2, 0, LocalDate.of(2026, 8, 21), 1, 3, 1);
        BorrowOrderVO order = new BorrowOrderVO(965L, "JY20260814000298", 42L, "zhang", "张三",
                1, "训练", LocalDate.of(2026, 8, 21), 2, 2, 0, null, null, null, List.of(item));
        when(borrowService.myPage(1, 50, null)).thenReturn(new PageResult<>(1, 1, 50, List.of(order)));
        BorrowTool tool = new BorrowTool(borrowService, configService);

        AiToolOutcome outcome = tool.getUserBorrowRecords(null);

        assertTrue(outcome.ok());
        List<BorrowTool.BorrowRecordSummary> records = cast(outcome.data());
        assertEquals(1, records.size());
        BorrowTool.BorrowRecordSummary record = records.get(0);
        assertEquals("JY20260814000298", record.orderNo());
        assertEquals("借用中", record.status());
        assertEquals("2026-08-21", record.expectedReturnDate());
        assertEquals("篮球", record.items().get(0).equipmentName());
        assertTrue(record.items().get(0).overdue());
        assertEquals(3, record.items().get(0).overdueDays());
        verify(borrowService).myPage(1, 50, null);
    }

    @Test
    void records_shouldPassStatusFilterThrough() {
        BorrowService borrowService = mock(BorrowService.class);
        SysConfigService configService = mock(SysConfigService.class);
        login(42L);
        when(borrowService.myPage(1, 50, 2)).thenReturn(new PageResult<>(0, 1, 50, List.of()));
        BorrowTool tool = new BorrowTool(borrowService, configService);

        AiToolOutcome outcome = tool.getUserBorrowRecords(2);

        assertTrue(outcome.ok());
        verify(borrowService).myPage(1, 50, 2);
    }

    @Test
    void records_shouldRejectInvalidStatus() {
        BorrowService borrowService = mock(BorrowService.class);
        SysConfigService configService = mock(SysConfigService.class);
        login(42L);
        BorrowTool tool = new BorrowTool(borrowService, configService);

        AiToolOutcome outcome = tool.getUserBorrowRecords(9);

        assertFalse(outcome.ok());
        assertTrue(outcome.message().contains("状态"));
        verify(borrowService, never()).myPage(anyLong(), anyLong(), any());
    }

    @Test
    void rule_shouldReturnRealConfigValues() {
        BorrowService borrowService = mock(BorrowService.class);
        SysConfigService configService = mock(SysConfigService.class);
        when(configService.getValueByKey("borrow.max.days")).thenReturn("15");
        when(configService.getValueByKey("borrow.overdue.penalty")).thenReturn("5");
        when(configService.getValueByKey("borrow.extend.limit")).thenReturn("1");
        BorrowTool tool = new BorrowTool(borrowService, configService);

        AiToolOutcome outcome = tool.getBorrowRule();

        assertTrue(outcome.ok());
        BorrowTool.BorrowRule rule = (BorrowTool.BorrowRule) outcome.data();
        assertEquals("15", rule.maxBorrowDays());
        assertEquals("5", rule.overduePenaltyPerDay());
        assertEquals("1", rule.extendLimit());
    }

    @Test
    void rule_shouldMarkMissingConfigAsUnconfigured() {
        BorrowService borrowService = mock(BorrowService.class);
        SysConfigService configService = mock(SysConfigService.class);
        when(configService.getValueByKey(any())).thenReturn(null);
        BorrowTool tool = new BorrowTool(borrowService, configService);

        AiToolOutcome outcome = tool.getBorrowRule();

        assertTrue(outcome.ok());
        BorrowTool.BorrowRule rule = (BorrowTool.BorrowRule) outcome.data();
        assertEquals("未配置", rule.maxBorrowDays());
    }

    @SuppressWarnings("unchecked")
    private List<BorrowTool.BorrowRecordSummary> cast(Object data) {
        return (List<BorrowTool.BorrowRecordSummary>) data;
    }

    private void login(Long userId) {
        LoginUser user = new LoginUser();
        user.setUserId(userId);
        user.setUsername("zhang");
        user.setPermissions(Set.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
