package com.company.sportseq.service;

import com.company.sportseq.dto.DeptDTO;
import com.company.sportseq.vo.DeptVO;

import java.util.List;

public interface SysDeptService {

    List<DeptVO> tree(String deptName);

    void add(DeptDTO dto);

    void update(DeptDTO dto);

    void remove(Long id);
}
