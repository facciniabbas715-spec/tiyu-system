package com.company.sportseq.service;

import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.dto.ScrapAuditDTO;
import com.company.sportseq.dto.ScrapDisposeDTO;
import com.company.sportseq.dto.ScrapOrderDTO;
import com.company.sportseq.vo.ScrapOrderVO;

public interface ScrapService {

    PageResult<ScrapOrderVO> page(long current, long size, String orderNo, Integer status);

    ScrapOrderVO detail(Long id);

    void create(ScrapOrderDTO dto);

    void audit(ScrapAuditDTO dto);

    void dispose(ScrapDisposeDTO dto);

    void cancel(Long id);
}
