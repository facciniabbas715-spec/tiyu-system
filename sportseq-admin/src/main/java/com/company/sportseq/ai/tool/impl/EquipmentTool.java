package com.company.sportseq.ai.tool.impl;

import com.company.sportseq.ai.tool.AiTool;
import com.company.sportseq.ai.tool.AiToolOutcome;
import com.company.sportseq.ai.tool.AiToolPermission;
import com.company.sportseq.common.exception.BizException;
import com.company.sportseq.service.EquipmentService;
import com.company.sportseq.vo.EquipmentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * 器材工具：读取器材档案（详情 / 按名称搜索），数据来自 {@link EquipmentService} 只读方法。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@AiToolPermission("equipment:list")
public class EquipmentTool implements AiTool {

    private final EquipmentService equipmentService;

    @Tool(description = "按器材ID查询器材档案详情，返回编码、名称、分类、品牌型号、规格、单位、价格、安全库存、最长借用天数、状态等信息")
    public AiToolOutcome getEquipmentDetail(
            @ToolParam(description = "器材ID") Long equipmentId) {
        if (equipmentId == null) {
            return AiToolOutcome.failure("请提供要查询的器材ID。");
        }
        try {
            EquipmentVO vo = equipmentService.detail(equipmentId);
            return AiToolOutcome.success(new EquipmentDetail(vo.id(), vo.equipmentCode(),
                    vo.equipmentName(), vo.categoryName(), vo.brand(), vo.model(), vo.spec(),
                    vo.unit(), vo.purchasePrice(), vo.safeStock(), vo.maxBorrowDays(),
                    vo.status(), vo.description()));
        } catch (BizException e) {
            return AiToolOutcome.failure(e.getMessage());
        } catch (Exception e) {
            log.warn("器材详情工具调用失败，equipmentId={}", equipmentId, e);
            return AiToolOutcome.failure("器材查询失败，请稍后重试。");
        }
    }

    @Tool(description = "按名称关键字搜索器材档案，返回匹配器材的简要信息（含器材ID，便于进一步查详情）")
    public AiToolOutcome searchEquipment(
            @ToolParam(description = "器材名称关键字，例如：篮球") String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return AiToolOutcome.failure("请提供要搜索的器材名称关键字。");
        }
        String trimmed = keyword.trim();
        try {
            List<EquipmentBrief> briefs = equipmentService.page(1, 20, trimmed,
                            null, null, null).records().stream()
                    .map(vo -> new EquipmentBrief(vo.id(), vo.equipmentCode(), vo.equipmentName(),
                            vo.categoryName(), vo.unit(), vo.maxBorrowDays(), vo.status()))
                    .toList();
            if (briefs.isEmpty()) {
                return AiToolOutcome.failure("未查询到名称包含「" + trimmed + "」的器材。");
            }
            return AiToolOutcome.success(briefs);
        } catch (Exception e) {
            log.warn("器材搜索工具调用失败，keyword={}", trimmed, e);
            return AiToolOutcome.failure("器材查询失败，请稍后重试。");
        }
    }

    /**
     * 器材档案详情。
     */
    public record EquipmentDetail(Long id, String equipmentCode, String equipmentName,
                                  String categoryName, String brand, String model, String spec,
                                  String unit, BigDecimal purchasePrice, Integer safeStock,
                                  Integer maxBorrowDays, Integer status, String description) {
    }

    /**
     * 器材简要信息。
     */
    public record EquipmentBrief(Long id, String equipmentCode, String equipmentName,
                                 String categoryName, String unit, Integer maxBorrowDays,
                                 Integer status) {
    }
}
