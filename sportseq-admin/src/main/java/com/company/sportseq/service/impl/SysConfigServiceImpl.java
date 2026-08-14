package com.company.sportseq.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.sportseq.common.constant.CacheConstants;
import com.company.sportseq.common.exception.BizException;
import com.company.sportseq.common.exception.ErrorCode;
import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.dto.ConfigDTO;
import com.company.sportseq.entity.SysConfig;
import com.company.sportseq.mapper.SysConfigMapper;
import com.company.sportseq.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    private final SysConfigMapper configMapper;

    @Override
    public PageResult<SysConfig> page(long current, long size, String configName, String configKey) {
        Page<SysConfig> page = configMapper.selectPage(new Page<>(current, size),
                Wrappers.<SysConfig>lambdaQuery()
                        .like(StrUtil.isNotBlank(configName), SysConfig::getConfigName, configName)
                        .like(StrUtil.isNotBlank(configKey), SysConfig::getConfigKey, configKey)
                        .orderByDesc(SysConfig::getCreateTime));
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    @Override
    public void add(ConfigDTO dto) {
        checkKeyUnique(dto.getConfigKey(), null);
        SysConfig config = new SysConfig();
        applyConfig(config, dto);
        configMapper.insert(config);
    }

    @Override
    @CacheEvict(cacheNames = CacheConstants.CONFIG_CACHE, allEntries = true)
    public void update(ConfigDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "参数ID不能为空");
        }
        checkKeyUnique(dto.getConfigKey(), dto.getId());
        SysConfig config = new SysConfig();
        config.setId(dto.getId());
        applyConfig(config, dto);
        configMapper.updateById(config);
    }

    @Override
    @CacheEvict(cacheNames = CacheConstants.CONFIG_CACHE, allEntries = true)
    public void remove(Long id) {
        configMapper.deleteById(id);
    }

    @Override
    @Cacheable(cacheNames = CacheConstants.CONFIG_CACHE, key = "#configKey")
    public String getValueByKey(String configKey) {
        SysConfig config = configMapper.selectOne(
                Wrappers.<SysConfig>lambdaQuery().eq(SysConfig::getConfigKey, configKey));
        return config == null ? null : config.getConfigValue();
    }

    private void checkKeyUnique(String configKey, Long excludeId) {
        Long count = configMapper.selectCount(Wrappers.<SysConfig>lambdaQuery()
                .eq(SysConfig::getConfigKey, configKey)
                .ne(excludeId != null, SysConfig::getId, excludeId));
        if (count > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "参数键已存在");
        }
    }

    private void applyConfig(SysConfig config, ConfigDTO dto) {
        config.setConfigName(dto.getConfigName());
        config.setConfigKey(dto.getConfigKey());
        config.setConfigValue(dto.getConfigValue());
        config.setConfigType(dto.getConfigType() == null ? "N" : dto.getConfigType());
        config.setRemark(dto.getRemark());
    }
}
