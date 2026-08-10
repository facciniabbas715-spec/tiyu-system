package com.company.sportseq.service;

import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.dto.StockAdjustDTO;
import com.company.sportseq.vo.StockRecordVO;
import com.company.sportseq.vo.StockVO;

import java.util.List;

public interface StockService {

    PageResult<StockVO> page(long current, long size, String equipmentName, String equipmentCode,
                             Long warehouseId, Long categoryId, Boolean warningOnly);

    PageResult<StockRecordVO> recordPage(long current, long size, Long equipmentId, Long warehouseId,
                                         Integer changeType);

    List<StockVO> warnings();

    void adjust(StockAdjustDTO dto);
}
