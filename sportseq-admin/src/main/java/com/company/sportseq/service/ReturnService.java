package com.company.sportseq.service;

import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.dto.ReturnOrderDTO;
import com.company.sportseq.vo.ReturnOrderVO;

public interface ReturnService {

    PageResult<ReturnOrderVO> page(long current, long size, String orderNo, Integer status);

    ReturnOrderVO detail(Long id);

    void create(ReturnOrderDTO dto);

    void confirm(Long id);

    void reject(Long id);
}
