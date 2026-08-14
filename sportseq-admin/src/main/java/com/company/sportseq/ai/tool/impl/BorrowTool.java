package com.company.sportseq.ai.tool.impl;

import com.company.sportseq.ai.tool.AiTool;
import com.company.sportseq.ai.tool.AiToolOutcome;
import com.company.sportseq.ai.tool.AiToolPermission;
import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.service.BorrowService;
import com.company.sportseq.service.SysConfigService;
import com.company.sportseq.vo.BorrowOrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 借用工具：查询当前登录用户本人的借用记录与系统借用规则参数。
 *
 * <p>借阅记录的用户身份由 {@link com.company.sportseq.security.SecurityUtils#getUserId()}
 * 在服务端绑定，模型无法传参指定他人，杜绝越权。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@AiToolPermission
public class BorrowTool implements AiTool {

    private static final int PAGE_SIZE = 100;

    private static final Map<Integer, String> STATUS_TEXT = Map.ofEntries(
            Map.entry(0, "待审核"),
            Map.entry(1, "已通过待领用"),
            Map.entry(2, "借用中"),
            Map.entry(3, "部分归还"),
            Map.entry(4, "已归还"),
            Map.entry(5, "已驳回"),
            Map.entry(6, "已取消"));

    private final BorrowService borrowService;

    private final SysConfigService configService;

    @Tool(description = "查询当前登录用户本人的借用记录，包括借了什么器材、数量、预计归还日期、状态、是否逾期；只能查本人数据，不能指定其他用户")
    public AiToolOutcome getUserBorrowRecords(
            @ToolParam(description = "借用单状态：0待审核 1已通过待领用 2借用中 3部分归还 4已归还 5已驳回 6已取消；不传则查全部", required = false)
            Integer status) {
        if (status != null && !STATUS_TEXT.containsKey(status)) {
            return AiToolOutcome.failure("借用状态参数无效，可选值：0-6。");
        }
        try {
            List<BorrowOrderVO> orders = new ArrayList<>();
            PageResult<BorrowOrderVO> page = borrowService.myPage(1, PAGE_SIZE, status);
            orders.addAll(page.records());
            while (orders.size() < page.total()) {
                page = borrowService.myPage(page.current() + 1, PAGE_SIZE, status);
                if (page.records().isEmpty()) {
                    break;
                }
                orders.addAll(page.records());
            }
            List<BorrowRecordSummary> records = orders.stream()
                    .map(this::toSummary)
                    .toList();
            return AiToolOutcome.success(records);
        } catch (Exception e) {
            log.warn("借用记录工具调用失败，status={}", status, e);
            return AiToolOutcome.failure("借用记录查询失败，请稍后重试。");
        }
    }

    @Tool(description = "查询系统借用规则参数：默认最长借用天数、逾期违约金（元/天）、续借次数上限，数据来自系统参数表")
    public AiToolOutcome getBorrowRule() {
        try {
            return AiToolOutcome.success(new BorrowRule(
                    valueOrUnset("borrow.max.days"),
                    valueOrUnset("borrow.overdue.penalty"),
                    valueOrUnset("borrow.extend.limit")));
        } catch (Exception e) {
            log.warn("借用规则工具调用失败", e);
            return AiToolOutcome.failure("借用规则查询失败，请稍后重试。");
        }
    }

    private BorrowRecordSummary toSummary(BorrowOrderVO order) {
        List<BorrowItemSummary> items = order.items() == null ? List.of()
                : order.items().stream()
                        .map(item -> new BorrowItemSummary(item.equipmentName(),
                                item.quantity(), item.returnedQuantity(),
                                item.expectedReturnDate() == null
                                        ? null : item.expectedReturnDate().toString(),
                                item.overdueFlag() != null && item.overdueFlag() == 1,
                                item.overdueDays()))
                        .toList();
        return new BorrowRecordSummary(order.orderNo(),
                STATUS_TEXT.getOrDefault(order.status(), "未知状态"),
                order.expectedReturnDate() == null
                        ? null : order.expectedReturnDate().toString(),
                items);
    }

    private String valueOrUnset(String key) {
        String value = configService.getValueByKey(key);
        return value == null ? "未配置" : value;
    }

    /**
     * 借用记录摘要。
     */
    public record BorrowRecordSummary(String orderNo, String status, String expectedReturnDate,
                                      List<BorrowItemSummary> items) {
    }

    /**
     * 借用明细摘要。
     */
    public record BorrowItemSummary(String equipmentName, Integer quantity,
                                    Integer returnedQuantity, String expectedReturnDate,
                                    boolean overdue, Integer overdueDays) {
    }

    /**
     * 系统借用规则参数。
     */
    public record BorrowRule(String maxBorrowDays, String overduePenaltyPerDay, String extendLimit) {
    }
}
