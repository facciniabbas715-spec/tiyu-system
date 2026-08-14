package com.company.sportseq.service.impl;

import com.alibaba.excel.EasyExcel;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.sportseq.common.cache.CacheService;
import com.company.sportseq.common.constant.CacheConstants;
import com.company.sportseq.common.exception.BizException;
import com.company.sportseq.common.exception.ErrorCode;
import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.dto.EquipmentDTO;
import com.company.sportseq.dto.EquipmentImportDTO;
import com.company.sportseq.entity.BorrowItem;
import com.company.sportseq.entity.Equipment;
import com.company.sportseq.entity.EquipmentCategory;
import com.company.sportseq.entity.EquipmentStock;
import com.company.sportseq.entity.ScrapItem;
import com.company.sportseq.mapper.BorrowItemMapper;
import com.company.sportseq.mapper.EquipmentCategoryMapper;
import com.company.sportseq.mapper.EquipmentMapper;
import com.company.sportseq.mapper.EquipmentStockMapper;
import com.company.sportseq.mapper.ScrapItemMapper;
import com.company.sportseq.service.EquipmentService;
import com.company.sportseq.vo.EquipmentVO;
import com.company.sportseq.vo.ImportResultVO;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EquipmentServiceImpl implements EquipmentService {

    private final EquipmentMapper equipmentMapper;
    private final EquipmentCategoryMapper categoryMapper;
    private final EquipmentStockMapper stockMapper;
    private final BorrowItemMapper borrowItemMapper;
    private final ScrapItemMapper scrapItemMapper;
    private final StringRedisTemplate redisTemplate;
    private final CacheService cacheService;

    @Override
    public PageResult<EquipmentVO> page(long current, long size, String equipmentName, String equipmentCode,
                                        Long categoryId, Integer status) {
        String cacheKey = CacheConstants.equipmentListKey(
                CacheConstants.hash(current, size, equipmentName, equipmentCode, categoryId, status));
        return cacheService.getOrLoad(cacheKey, new TypeReference<>() {
        }, CacheConstants.EQUIPMENT_LIST_TTL,
                () -> loadPage(current, size, equipmentName, equipmentCode, categoryId, status));
    }

    private PageResult<EquipmentVO> loadPage(long current, long size, String equipmentName, String equipmentCode,
                                             Long categoryId, Integer status) {
        Page<Equipment> page = equipmentMapper.selectPage(new Page<>(current, size),
                Wrappers.<Equipment>lambdaQuery()
                        .like(StrUtil.isNotBlank(equipmentName), Equipment::getEquipmentName, equipmentName)
                        .like(StrUtil.isNotBlank(equipmentCode), Equipment::getEquipmentCode, equipmentCode)
                        .eq(categoryId != null, Equipment::getCategoryId, categoryId)
                        .eq(status != null, Equipment::getStatus, status)
                        .orderByDesc(Equipment::getCreateTime));
        List<Equipment> records = page.getRecords();
        List<Long> categoryIds = records.stream()
                .map(Equipment::getCategoryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, EquipmentCategory> categoryMap = categoryIds.isEmpty() ? Map.of()
                : categoryMapper.selectBatchIds(categoryIds).stream()
                        .collect(Collectors.toMap(EquipmentCategory::getId, c -> c, (a, b) -> a));
        List<EquipmentVO> vos = records.stream()
                .map(e -> toVo(e, categoryMap))
                .toList();
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), vos);
    }

    @Override
    public EquipmentVO detail(Long id) {
        return cacheService.getOrLoad(CacheConstants.equipmentDetailKey(id), new TypeReference<>() {
        }, CacheConstants.EQUIPMENT_DETAIL_TTL, () -> {
            Equipment equipment = equipmentMapper.selectById(id);
        if (equipment == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "器材不存在");
        }
            return toVo(equipment);
        });
    }

    @Override
    public void add(EquipmentDTO dto) {
        EquipmentCategory category = validateCategory(dto.getCategoryId());
        Equipment equipment = new Equipment();
        applyFields(equipment, dto);
        equipment.setEquipmentCode(generateCode(category.getCategoryCode()));
        equipment.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        equipmentMapper.insert(equipment);
        cacheService.evictByPattern(CacheConstants.EQUIPMENT_LIST_PATTERN);
    }

    @Override
    public void update(EquipmentDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "器材ID不能为空");
        }
        validateCategory(dto.getCategoryId());
        Equipment equipment = new Equipment();
        equipment.setId(dto.getId());
        applyFields(equipment, dto);
        equipment.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        equipmentMapper.updateById(equipment);
        cacheService.evict(CacheConstants.equipmentDetailKey(dto.getId()));
        cacheService.evictByPattern(CacheConstants.EQUIPMENT_LIST_PATTERN);
        cacheService.evictByPattern(CacheConstants.STOCK_PAGE_PATTERN);
    }

    @Override
    public void remove(Long id) {
        Long stockCount = stockMapper.selectCount(Wrappers.<EquipmentStock>lambdaQuery()
                .eq(EquipmentStock::getEquipmentId, id)
                .apply("(quantity + locked_quantity) > 0"));
        if (stockCount > 0) {
            throw new BizException(ErrorCode.EQUIPMENT_IN_USE, "器材存在在库库存，无法删除");
        }
        Long borrowCount = borrowItemMapper.selectCount(Wrappers.<BorrowItem>lambdaQuery()
                .eq(BorrowItem::getEquipmentId, id)
                .inSql(BorrowItem::getBorrowId,
                        "SELECT id FROM borrow_order WHERE status IN (0,1,2,3)"));
        if (borrowCount > 0) {
            throw new BizException(ErrorCode.EQUIPMENT_IN_USE, "器材存在未完结借用单，无法删除");
        }
        Long scrapCount = scrapItemMapper.selectCount(Wrappers.<ScrapItem>lambdaQuery()
                .eq(ScrapItem::getEquipmentId, id)
                .inSql(ScrapItem::getScrapId,
                        "SELECT id FROM scrap_order WHERE status IN (0,1)"));
        if (scrapCount > 0) {
            throw new BizException(ErrorCode.EQUIPMENT_IN_USE, "器材存在未完结报废单，无法删除");
        }
        equipmentMapper.deleteById(id);
        cacheService.evict(CacheConstants.equipmentDetailKey(id));
        cacheService.evictByPattern(CacheConstants.EQUIPMENT_LIST_PATTERN);
        cacheService.evictByPattern(CacheConstants.STOCK_PAGE_PATTERN);
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        Equipment update = new Equipment();
        update.setId(id);
        update.setStatus(status);
        equipmentMapper.updateById(update);
        cacheService.evict(CacheConstants.equipmentDetailKey(id));
        cacheService.evictByPattern(CacheConstants.EQUIPMENT_LIST_PATTERN);
    }

    @Override
    public List<EquipmentVO> exportList(String equipmentName, String equipmentCode, Long categoryId, Integer status) {
        return equipmentMapper.selectList(Wrappers.<Equipment>lambdaQuery()
                        .like(StrUtil.isNotBlank(equipmentName), Equipment::getEquipmentName, equipmentName)
                        .like(StrUtil.isNotBlank(equipmentCode), Equipment::getEquipmentCode, equipmentCode)
                        .eq(categoryId != null, Equipment::getCategoryId, categoryId)
                        .eq(status != null, Equipment::getStatus, status)
                        .orderByDesc(Equipment::getCreateTime))
                .stream().map(this::toVo).toList();
    }

    @Override
    public ImportResultVO importExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "请选择要导入的Excel文件");
        }
        List<EquipmentImportDTO> rows;
        try {
            rows = EasyExcel.read(file.getInputStream())
                    .head(EquipmentImportDTO.class)
                    .sheet()
                    .doReadSync();
        } catch (IOException e) {
            throw new BizException(ErrorCode.PARAM_ERROR, "Excel解析失败: " + e.getMessage());
        }
        int success = 0;
        List<String> errors = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            EquipmentImportDTO row = rows.get(i);
            int rowNum = i + 2; // 表头占一行
            try {
                if (StrUtil.isBlank(row.getEquipmentName()) || StrUtil.isBlank(row.getCategoryCode())) {
                    throw new BizException(ErrorCode.PARAM_ERROR, "器材名称与分类编码不能为空");
                }
                EquipmentCategory category = categoryMapper.selectOne(
                        Wrappers.<EquipmentCategory>lambdaQuery()
                                .eq(EquipmentCategory::getCategoryCode, row.getCategoryCode().trim()));
                if (category == null) {
                    throw new BizException(ErrorCode.PARAM_ERROR, "分类编码不存在");
                }
                Equipment equipment = new Equipment();
                equipment.setEquipmentName(row.getEquipmentName().trim());
                equipment.setCategoryId(category.getId());
                equipment.setBrand(row.getBrand());
                equipment.setModel(row.getModel());
                equipment.setSpec(row.getSpec());
                equipment.setUnit(StrUtil.isBlank(row.getUnit()) ? "个" : row.getUnit());
                equipment.setPurchasePrice(row.getPurchasePrice());
                equipment.setSafeStock(row.getSafeStock() == null ? 0 : row.getSafeStock());
                equipment.setMaxBorrowDays(row.getMaxBorrowDays());
                equipment.setDescription(row.getDescription());
                equipment.setStatus(1);
                equipment.setEquipmentCode(generateCode(category.getCategoryCode()));
                equipmentMapper.insert(equipment);
                success++;
            } catch (Exception e) {
                errors.add("第" + rowNum + "行: " + e.getMessage());
            }
        }
        cacheService.evictByPattern(CacheConstants.EQUIPMENT_LIST_PATTERN);
        return new ImportResultVO(success, errors.size(), errors);
    }

    private EquipmentCategory validateCategory(Long categoryId) {
        EquipmentCategory category = cacheService.getOrLoad(CacheConstants.categoryKey(categoryId),
                new TypeReference<>() {
                }, CacheConstants.CATEGORY_TTL, () -> categoryMapper.selectById(categoryId));
        if (category == null || (category.getStatus() != null && category.getStatus() == 0)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "器材分类不存在或已停用");
        }
        return category;
    }

    /**
     * 生成器材编码：{分类码}-{年}-{6位流水}，Redis INCR 保证并发唯一，唯一键兜底。
     */
    public String generateCode(String categoryCode) {
        int year = Year.now().getValue();
        for (int i = 0; i < 5; i++) {
            Long seq = redisTemplate.opsForValue().increment(
                    CacheConstants.EQUIPMENT_CODE_KEY + categoryCode + ":" + year);
            String code = categoryCode + "-" + year + "-" + String.format("%06d", seq);
            Long exists = equipmentMapper.selectCount(
                    Wrappers.<Equipment>lambdaQuery().eq(Equipment::getEquipmentCode, code));
            if (exists == 0) {
                return code;
            }
        }
        throw new BizException(ErrorCode.SYSTEM_ERROR, "器材编码生成失败，请重试");
    }

    private void applyFields(Equipment equipment, EquipmentDTO dto) {
        equipment.setEquipmentName(dto.getEquipmentName());
        equipment.setCategoryId(dto.getCategoryId());
        equipment.setBrand(dto.getBrand());
        equipment.setModel(dto.getModel());
        equipment.setSpec(dto.getSpec());
        equipment.setUnit(dto.getUnit());
        equipment.setPurchasePrice(dto.getPurchasePrice());
        equipment.setSafeStock(dto.getSafeStock() == null ? 0 : dto.getSafeStock());
        equipment.setMaxBorrowDays(dto.getMaxBorrowDays());
        equipment.setImageUrl(dto.getImageUrl());
        equipment.setDescription(dto.getDescription());
    }

    private EquipmentVO toVo(Equipment equipment) {
        return toVo(equipment, Map.of());
    }

    private EquipmentVO toVo(Equipment equipment, Map<Long, EquipmentCategory> categoryMap) {
        String categoryName = null;
        if (equipment.getCategoryId() != null) {
            EquipmentCategory category = categoryMap.get(equipment.getCategoryId());
            categoryName = category == null ? null : category.getCategoryName();
        }
        return new EquipmentVO(equipment.getId(), equipment.getEquipmentCode(), equipment.getEquipmentName(),
                equipment.getCategoryId(), categoryName, equipment.getBrand(), equipment.getModel(),
                equipment.getSpec(), equipment.getUnit(), equipment.getPurchasePrice(),
                equipment.getSafeStock(), equipment.getMaxBorrowDays(), equipment.getImageUrl(),
                equipment.getStatus(), equipment.getDescription(), equipment.getCreateTime());
    }
}
