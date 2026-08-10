package com.company.sportseq.service;

import com.company.sportseq.dto.EquipmentCategoryDTO;
import com.company.sportseq.vo.EquipmentCategoryVO;

import java.util.List;

public interface EquipmentCategoryService {

    List<EquipmentCategoryVO> tree(String categoryName);

    void add(EquipmentCategoryDTO dto);

    void update(EquipmentCategoryDTO dto);

    void remove(Long id);
}
