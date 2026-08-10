package com.company.sportseq.service;

import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.dto.DictDataDTO;
import com.company.sportseq.dto.DictTypeDTO;
import com.company.sportseq.entity.SysDictData;
import com.company.sportseq.entity.SysDictType;

import java.util.List;

public interface SysDictService {

    PageResult<SysDictType> typePage(long current, long size, String dictName, String dictType);

    void addType(DictTypeDTO dto);

    void updateType(DictTypeDTO dto);

    void removeType(Long id);

    PageResult<SysDictData> dataPage(long current, long size, String dictType);

    void addData(DictDataDTO dto);

    void updateData(DictDataDTO dto);

    void removeData(Long id);

    List<SysDictData> listByType(String dictType);
}
