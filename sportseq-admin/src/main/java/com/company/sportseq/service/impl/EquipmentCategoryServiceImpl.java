package com.company.sportseq.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.company.sportseq.common.cache.CacheService;
import com.company.sportseq.common.constant.CacheConstants;
import com.company.sportseq.common.exception.BizException;
import com.company.sportseq.common.exception.ErrorCode;
import com.company.sportseq.dto.EquipmentCategoryDTO;
import com.company.sportseq.entity.Equipment;
import com.company.sportseq.entity.EquipmentCategory;
import com.company.sportseq.mapper.EquipmentCategoryMapper;
import com.company.sportseq.mapper.EquipmentMapper;
import com.company.sportseq.service.EquipmentCategoryService;
import com.company.sportseq.vo.EquipmentCategoryVO;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EquipmentCategoryServiceImpl implements EquipmentCategoryService {

    private final EquipmentCategoryMapper categoryMapper;
    private final EquipmentMapper equipmentMapper;
    private final CacheService cacheService;

    @Override
    public List<EquipmentCategoryVO> tree(String categoryName) {
        // 无过滤条件时缓存全量列表（分类树/下拉高频读取），带过滤条件仍走 SQL 保持 LIKE 语义
        List<EquipmentCategory> categories = StrUtil.isBlank(categoryName)
                ? cacheService.getOrLoad(CacheConstants.CATEGORY_LIST_KEY,
                        new TypeReference<>() {
                        }, CacheConstants.CATEGORY_TTL,
                        () -> categoryMapper.selectList(Wrappers.<EquipmentCategory>lambdaQuery()
                                .orderByAsc(EquipmentCategory::getSortOrder)))
                : categoryMapper.selectList(Wrappers.<EquipmentCategory>lambdaQuery()
                        .like(EquipmentCategory::getCategoryName, categoryName)
                        .orderByAsc(EquipmentCategory::getSortOrder));
        Map<Long, EquipmentCategoryVO> voMap = categories.stream()
                .collect(Collectors.toMap(EquipmentCategory::getId, this::toVo));
        List<EquipmentCategoryVO> roots = new ArrayList<>();
        for (EquipmentCategory category : categories) {
            EquipmentCategoryVO vo = voMap.get(category.getId());
            EquipmentCategoryVO parent = voMap.get(category.getParentId());
            if (parent == null) {
                roots.add(vo);
            } else {
                parent.children().add(vo);
            }
        }
        return roots;
    }

    @Override
    public void add(EquipmentCategoryDTO dto) {
        checkCodeUnique(dto.getCategoryCode(), null);
        EquipmentCategory category = new EquipmentCategory();
        category.setParentId(dto.getParentId() == null ? 0 : dto.getParentId());
        category.setAncestors(buildAncestors(category.getParentId()));
        category.setCategoryName(dto.getCategoryName());
        category.setCategoryCode(dto.getCategoryCode());
        category.setIcon(dto.getIcon());
        category.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        category.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        category.setRemark(dto.getRemark());
        categoryMapper.insert(category);
        cacheService.evict(CacheConstants.CATEGORY_LIST_KEY);
    }

    @Override
    public void update(EquipmentCategoryDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "分类ID不能为空");
        }
        checkCodeUnique(dto.getCategoryCode(), dto.getId());
        EquipmentCategory category = new EquipmentCategory();
        category.setId(dto.getId());
        category.setParentId(dto.getParentId() == null ? 0 : dto.getParentId());
        category.setAncestors(buildAncestors(category.getParentId()));
        category.setCategoryName(dto.getCategoryName());
        category.setCategoryCode(dto.getCategoryCode());
        category.setIcon(dto.getIcon());
        category.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        category.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        category.setRemark(dto.getRemark());
        categoryMapper.updateById(category);
        evictCategoryRelated(dto.getId());
    }

    @Override
    public void remove(Long id) {
        Long children = categoryMapper.selectCount(
                Wrappers.<EquipmentCategory>lambdaQuery().eq(EquipmentCategory::getParentId, id));
        if (children > 0) {
            throw new BizException(ErrorCode.CATEGORY_HAS_CHILDREN);
        }
        Long equipmentCount = equipmentMapper.selectCount(
                Wrappers.<Equipment>lambdaQuery().eq(Equipment::getCategoryId, id));
        if (equipmentCount > 0) {
            throw new BizException(ErrorCode.EQUIPMENT_IN_USE);
        }
        categoryMapper.deleteById(id);
        evictCategoryRelated(id);
    }

    /** 分类变化会影响分类缓存、器材列表/库存分页中展示的分类名称 */
    private void evictCategoryRelated(Long categoryId) {
        cacheService.evict(CacheConstants.CATEGORY_LIST_KEY, CacheConstants.categoryKey(categoryId));
        cacheService.evictByPattern(CacheConstants.EQUIPMENT_LIST_PATTERN);
        cacheService.evictByPattern(CacheConstants.STOCK_PAGE_PATTERN);
    }

    private void checkCodeUnique(String categoryCode, Long excludeId) {
        Long count = categoryMapper.selectCount(Wrappers.<EquipmentCategory>lambdaQuery()
                .eq(EquipmentCategory::getCategoryCode, categoryCode)
                .ne(excludeId != null, EquipmentCategory::getId, excludeId));
        if (count > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "分类编码已存在");
        }
    }

    private String buildAncestors(Long parentId) {
        if (parentId == null || parentId == 0) {
            return "0";
        }
        EquipmentCategory parent = categoryMapper.selectById(parentId);
        if (parent == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "父分类不存在");
        }
        return parent.getAncestors() + "," + parentId;
    }

    private EquipmentCategoryVO toVo(EquipmentCategory category) {
        return new EquipmentCategoryVO(category.getId(), category.getParentId(),
                category.getCategoryName(), category.getCategoryCode(), category.getIcon(),
                category.getSortOrder(), category.getStatus(), new ArrayList<>());
    }
}
