package com.company.sportseq.service;

import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.dto.StockInAuditDTO;
import com.company.sportseq.dto.StockInOrderDTO;
import com.company.sportseq.vo.StockInVO;

public interface StockInService {

    PageResult<StockInVO> page(long current, long size, String orderNo, Integer status);

    StockInVO detail(Long id);

    void create(StockInOrderDTO dto);

    void submit(Long id);

    void audit(StockInAuditDTO dto);

    void receive(Long id);

    void cancel(Long id);

    void remove(Long id);
}
