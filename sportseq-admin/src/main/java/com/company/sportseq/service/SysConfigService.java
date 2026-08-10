package com.company.sportseq.service;

import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.dto.ConfigDTO;
import com.company.sportseq.entity.SysConfig;

public interface SysConfigService {

    PageResult<SysConfig> page(long current, long size, String configName, String configKey);

    void add(ConfigDTO dto);

    void update(ConfigDTO dto);

    void remove(Long id);

    String getValueByKey(String configKey);
}
