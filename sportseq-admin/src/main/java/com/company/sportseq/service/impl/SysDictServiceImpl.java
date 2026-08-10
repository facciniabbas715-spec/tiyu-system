package com.company.sportseq.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.sportseq.common.exception.BizException;
import com.company.sportseq.common.exception.ErrorCode;
import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.dto.DictDataDTO;
import com.company.sportseq.dto.DictTypeDTO;
import com.company.sportseq.entity.SysDictData;
import com.company.sportseq.entity.SysDictType;
import com.company.sportseq.mapper.SysDictDataMapper;
import com.company.sportseq.mapper.SysDictTypeMapper;
import com.company.sportseq.service.SysDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysDictServiceImpl implements SysDictService {

    private final SysDictTypeMapper typeMapper;
    private final SysDictDataMapper dataMapper;

    @Override
    public PageResult<SysDictType> typePage(long current, long size, String dictName, String dictType) {
        Page<SysDictType> page = typeMapper.selectPage(new Page<>(current, size),
                Wrappers.<SysDictType>lambdaQuery()
                        .like(StrUtil.isNotBlank(dictName), SysDictType::getDictName, dictName)
                        .like(StrUtil.isNotBlank(dictType), SysDictType::getDictType, dictType)
                        .orderByDesc(SysDictType::getCreateTime));
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    @Override
    public void addType(DictTypeDTO dto) {
        checkTypeUnique(dto.getDictType(), null);
        SysDictType type = new SysDictType();
        type.setDictName(dto.getDictName());
        type.setDictType(dto.getDictType());
        type.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        type.setRemark(dto.getRemark());
        typeMapper.insert(type);
    }

    @Override
    @CacheEvict(cacheNames = "dict", allEntries = true)
    public void updateType(DictTypeDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "字典类型ID不能为空");
        }
        checkTypeUnique(dto.getDictType(), dto.getId());
        SysDictType type = new SysDictType();
        type.setId(dto.getId());
        type.setDictName(dto.getDictName());
        type.setDictType(dto.getDictType());
        type.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        type.setRemark(dto.getRemark());
        typeMapper.updateById(type);
    }

    @Override
    public void removeType(Long id) {
        SysDictType type = typeMapper.selectById(id);
        if (type == null) {
            return;
        }
        Long dataCount = dataMapper.selectCount(
                Wrappers.<SysDictData>lambdaQuery().eq(SysDictData::getDictType, type.getDictType()));
        if (dataCount > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "字典类型下存在字典数据，无法删除");
        }
        typeMapper.deleteById(id);
    }

    @Override
    public PageResult<SysDictData> dataPage(long current, long size, String dictType) {
        Page<SysDictData> page = dataMapper.selectPage(new Page<>(current, size),
                Wrappers.<SysDictData>lambdaQuery()
                        .eq(StrUtil.isNotBlank(dictType), SysDictData::getDictType, dictType)
                        .orderByAsc(SysDictData::getDictSort));
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    @Override
    @CacheEvict(cacheNames = "dict", allEntries = true)
    public void addData(DictDataDTO dto) {
        SysDictData data = new SysDictData();
        applyData(data, dto);
        dataMapper.insert(data);
    }

    @Override
    @CacheEvict(cacheNames = "dict", allEntries = true)
    public void updateData(DictDataDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "字典数据ID不能为空");
        }
        SysDictData data = new SysDictData();
        data.setId(dto.getId());
        applyData(data, dto);
        dataMapper.updateById(data);
    }

    @Override
    public void removeData(Long id) {
        dataMapper.deleteById(id);
    }

    @Override
    @Cacheable(cacheNames = "dict", key = "'data:' + #dictType")
    public List<SysDictData> listByType(String dictType) {
        return dataMapper.selectList(Wrappers.<SysDictData>lambdaQuery()
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getStatus, 1)
                .orderByAsc(SysDictData::getDictSort));
    }

    private void checkTypeUnique(String dictType, Long excludeId) {
        Long count = typeMapper.selectCount(Wrappers.<SysDictType>lambdaQuery()
                .eq(SysDictType::getDictType, dictType)
                .ne(excludeId != null, SysDictType::getId, excludeId));
        if (count > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "字典类型已存在");
        }
    }

    private void applyData(SysDictData data, DictDataDTO dto) {
        data.setDictType(dto.getDictType());
        data.setDictSort(dto.getDictSort() == null ? 0 : dto.getDictSort());
        data.setDictLabel(dto.getDictLabel());
        data.setDictValue(dto.getDictValue());
        data.setCssClass(dto.getCssClass());
        data.setListClass(dto.getListClass());
        data.setIsDefault(dto.getIsDefault() == null ? "N" : dto.getIsDefault());
        data.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        data.setRemark(dto.getRemark());
    }
}
