package com.company.sportseq.service;

import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.dto.BorrowAuditDTO;
import com.company.sportseq.dto.BorrowExtendDTO;
import com.company.sportseq.dto.BorrowOrderDTO;
import com.company.sportseq.vo.BorrowOrderVO;

public interface BorrowService {

    PageResult<BorrowOrderVO> page(long current, long size, String orderNo, String username,
                                   Integer status, Long borrowUserId);

    PageResult<BorrowOrderVO> myPage(long current, long size, Integer status);

    BorrowOrderVO detail(Long id);

    void create(BorrowOrderDTO dto);

    void audit(BorrowAuditDTO dto);

    void issue(Long id);

    void extend(BorrowExtendDTO dto);

    void cancel(Long id);
}
