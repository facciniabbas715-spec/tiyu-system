package com.company.sportseq.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.company.sportseq.common.exception.BizException;
import com.company.sportseq.common.exception.ErrorCode;
import com.company.sportseq.dto.WarehouseDTO;
import com.company.sportseq.entity.Warehouse;
import com.company.sportseq.mapper.WarehouseMapper;
import com.company.sportseq.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseMapper warehouseMapper;

    @Override
    public List<Warehouse> list() {
        return warehouseMapper.selectList(Wrappers.<Warehouse>lambdaQuery()
                .orderByAsc(Warehouse::getId));
    }

    @Override
    public void add(WarehouseDTO dto) {
        checkCodeUnique(dto.getWarehouseCode(), null);
        Warehouse warehouse = new Warehouse();
        applyFields(warehouse, dto);
        warehouseMapper.insert(warehouse);
    }

    @Override
    public void update(WarehouseDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "仓库ID不能为空");
        }
        checkCodeUnique(dto.getWarehouseCode(), dto.getId());
        Warehouse warehouse = new Warehouse();
        warehouse.setId(dto.getId());
        applyFields(warehouse, dto);
        warehouseMapper.updateById(warehouse);
    }

    @Override
    public void remove(Long id) {
        warehouseMapper.deleteById(id);
    }

    private void checkCodeUnique(String warehouseCode, Long excludeId) {
        Long count = warehouseMapper.selectCount(Wrappers.<Warehouse>lambdaQuery()
                .eq(Warehouse::getWarehouseCode, warehouseCode)
                .ne(excludeId != null, Warehouse::getId, excludeId));
        if (count > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "仓库编码已存在");
        }
    }

    private void applyFields(Warehouse warehouse, WarehouseDTO dto) {
        warehouse.setWarehouseCode(dto.getWarehouseCode());
        warehouse.setWarehouseName(dto.getWarehouseName());
        warehouse.setManager(dto.getManager());
        warehouse.setPhone(dto.getPhone());
        warehouse.setAddress(dto.getAddress());
        warehouse.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        warehouse.setRemark(dto.getRemark());
    }
}
