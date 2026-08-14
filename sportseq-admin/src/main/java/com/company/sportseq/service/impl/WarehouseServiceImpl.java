package com.company.sportseq.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.company.sportseq.common.cache.CacheService;
import com.company.sportseq.common.constant.CacheConstants;
import com.company.sportseq.common.exception.BizException;
import com.company.sportseq.common.exception.ErrorCode;
import com.company.sportseq.dto.WarehouseDTO;
import com.company.sportseq.entity.EquipmentStock;
import com.company.sportseq.entity.ReturnOrder;
import com.company.sportseq.entity.ScrapOrder;
import com.company.sportseq.entity.StockInOrder;
import com.company.sportseq.entity.Warehouse;
import com.company.sportseq.mapper.EquipmentStockMapper;
import com.company.sportseq.mapper.ReturnOrderMapper;
import com.company.sportseq.mapper.ScrapOrderMapper;
import com.company.sportseq.mapper.StockInOrderMapper;
import com.company.sportseq.mapper.WarehouseMapper;
import com.company.sportseq.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseMapper warehouseMapper;
    private final EquipmentStockMapper stockMapper;
    private final StockInOrderMapper stockInOrderMapper;
    private final ReturnOrderMapper returnOrderMapper;
    private final ScrapOrderMapper scrapOrderMapper;
    private final CacheService cacheService;

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
        cacheService.evictByPattern(CacheConstants.STOCK_PAGE_PATTERN);
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
        cacheService.evictByPattern(CacheConstants.STOCK_PAGE_PATTERN);
    }

    @Override
    public void remove(Long id) {
        Long stockCount = stockMapper.selectCount(
                Wrappers.<EquipmentStock>lambdaQuery().eq(EquipmentStock::getWarehouseId, id));
        if (stockCount > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "仓库存在库存记录，无法删除");
        }
        Long stockInCount = stockInOrderMapper.selectCount(
                Wrappers.<StockInOrder>lambdaQuery().eq(StockInOrder::getWarehouseId, id));
        if (stockInCount > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "仓库存在入库单，无法删除");
        }
        Long returnCount = returnOrderMapper.selectCount(
                Wrappers.<ReturnOrder>lambdaQuery().eq(ReturnOrder::getWarehouseId, id));
        if (returnCount > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "仓库存在归还单，无法删除");
        }
        Long scrapCount = scrapOrderMapper.selectCount(
                Wrappers.<ScrapOrder>lambdaQuery().eq(ScrapOrder::getWarehouseId, id));
        if (scrapCount > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "仓库存在报废单，无法删除");
        }
        warehouseMapper.deleteById(id);
        cacheService.evictByPattern(CacheConstants.STOCK_PAGE_PATTERN);
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
