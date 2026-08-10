package com.company.sportseq.service;

import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.dto.EquipmentDTO;
import com.company.sportseq.dto.EquipmentImportDTO;
import com.company.sportseq.vo.EquipmentVO;
import com.company.sportseq.vo.ImportResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface EquipmentService {

    PageResult<EquipmentVO> page(long current, long size, String equipmentName, String equipmentCode,
                                 Long categoryId, Integer status);

    EquipmentVO detail(Long id);

    void add(EquipmentDTO dto);

    void update(EquipmentDTO dto);

    void remove(Long id);

    void changeStatus(Long id, Integer status);

    List<EquipmentVO> exportList(String equipmentName, String equipmentCode, Long categoryId, Integer status);

    ImportResultVO importExcel(MultipartFile file);
}
