package com.company.sportseq.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.sportseq.common.exception.BizException;
import com.company.sportseq.common.exception.ErrorCode;
import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.dto.StockAdjustDTO;
import com.company.sportseq.entity.Equipment;
import com.company.sportseq.entity.EquipmentCategory;
import com.company.sportseq.entity.EquipmentStock;
import com.company.sportseq.entity.StockRecord;
import com.company.sportseq.entity.Warehouse;
import com.company.sportseq.mapper.EquipmentCategoryMapper;
import com.company.sportseq.mapper.EquipmentMapper;
import com.company.sportseq.mapper.EquipmentStockMapper;
import com.company.sportseq.mapper.StockRecordMapper;
import com.company.sportseq.mapper.WarehouseMapper;
import com.company.sportseq.security.SecurityUtils;
import com.company.sportseq.service.StockService;
import com.company.sportseq.vo.StockRecordVO;
import com.company.sportseq.vo.StockVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final EquipmentStockMapper stockMapper;
    private final EquipmentMapper equipmentMapper;
    private final EquipmentCategoryMapper categoryMapper;
    private final WarehouseMapper warehouseMapper;
    private final StockRecordMapper recordMapper;

    @Override
    public PageResult<StockVO> page(long current, long size, String equipmentName, String equipmentCode,
                                    Long warehouseId, Long categoryId, Boolean warningOnly) {
        Page<EquipmentStock> page = stockMapper.selectPage(new Page<>(current, size),
                Wrappers.<EquipmentStock>lambdaQuery()
                        .eq(warehouseId != null, EquipmentStock::getWarehouseId, warehouseId)
                        .orderByDesc(EquipmentStock::getUpdateTime));
        List<StockVO> records = page.getRecords().stream()
                .filter(stock -> filterEquipment(stock.getEquipmentId(), equipmentName, equipmentCode, categoryId))
                .filter(stock -> !Boolean.TRUE.equals(warningOnly) || isWarning(stock))
                .map(this::toVo)
                .toList();
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    public PageResult<StockRecordVO> recordPage(long current, long size, Long equipmentId, Long warehouseId,
                                                Integer changeType) {
        Page<StockRecord> page = recordMapper.selectPage(new Page<>(current, size),
                Wrappers.<StockRecord>lambdaQuery()
                        .eq(equipmentId != null, StockRecord::getEquipmentId, equipmentId)
                        .eq(warehouseId != null, StockRecord::getWarehouseId, warehouseId)
                        .eq(changeType != null, StockRecord::getChangeType, changeType)
                        .orderByDesc(StockRecord::getCreateTime));
        List<StockRecordVO> records = page.getRecords().stream().map(this::toRecordVo).toList();
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    public List<StockVO> warnings() {
        return stockMapper.selectList(null).stream()
                .filter(this::isWarning)
                .map(this::toVo)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjust(StockAdjustDTO dto) {
        if (dto.getChangeQuantity() == 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "调整数量不能为0");
        }
        Equipment equipment = equipmentMapper.selectById(dto.getEquipmentId());
        if (equipment == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "器材不存在");
        }
        Long userId = SecurityUtils.getUserId();
        EquipmentStock stock = stockMapper.selectForUpdate(dto.getEquipmentId(), dto.getWarehouseId());
        if (stock == null) {
            if (dto.getChangeQuantity() < 0) {
                throw new BizException(ErrorCode.STOCK_NOT_ENOUGH);
            }
            stock = new EquipmentStock();
            stock.setEquipmentId(dto.getEquipmentId());
            stock.setWarehouseId(dto.getWarehouseId());
            stock.setQuantity(0);
            stock.setLockedQuantity(0);
            stock.setVersion(0);
            stockMapper.insert(stock);
        }
        int before = stock.getQuantity();
        if (dto.getChangeQuantity() > 0) {
            stockMapper.addQuantity(dto.getEquipmentId(), dto.getWarehouseId(),
                    dto.getChangeQuantity(), userId);
        } else {
            int affected = stockMapper.subtractQuantity(dto.getEquipmentId(), dto.getWarehouseId(),
                    -dto.getChangeQuantity(), userId);
            if (affected == 0) {
                throw new BizException(ErrorCode.STOCK_NOT_ENOUGH);
            }
        }
        StockRecord record = new StockRecord();
        record.setEquipmentId(dto.getEquipmentId());
        record.setWarehouseId(dto.getWarehouseId());
        record.setChangeType(dto.getChangeQuantity() > 0 ? 5 : 6);
        record.setChangeQuantity(dto.getChangeQuantity());
        record.setBeforeQuantity(before);
        record.setAfterQuantity(before + dto.getChangeQuantity());
        record.setRefOrderType("STOCKTAKING");
        record.setRemark(dto.getRemark());
        record.setCreateBy(userId);
        record.setCreateTime(LocalDateTime.now());
        recordMapper.insert(record);
    }

    private boolean filterEquipment(Long equipmentId, String name, String code, Long categoryId) {
        if (StrUtil.isBlank(name) && StrUtil.isBlank(code) && categoryId == null) {
            return true;
        }
        Equipment equipment = equipmentMapper.selectById(equipmentId);
        if (equipment == null) {
            return false;
        }
        if (StrUtil.isNotBlank(name) && !equipment.getEquipmentName().contains(name)) {
            return false;
        }
        if (StrUtil.isNotBlank(code) && !equipment.getEquipmentCode().contains(code)) {
            return false;
        }
        return categoryId == null || categoryId.equals(equipment.getCategoryId());
    }

    private boolean isWarning(EquipmentStock stock) {
        Equipment equipment = equipmentMapper.selectById(stock.getEquipmentId());
        return equipment != null && stock.getQuantity() <= (equipment.getSafeStock() == null ? 0 : equipment.getSafeStock());
    }

    private StockVO toVo(EquipmentStock stock) {
        Equipment equipment = equipmentMapper.selectById(stock.getEquipmentId());
        Warehouse warehouse = warehouseMapper.selectById(stock.getWarehouseId());
        String categoryName = null;
        if (equipment != null && equipment.getCategoryId() != null) {
            EquipmentCategory category = categoryMapper.selectById(equipment.getCategoryId());
            categoryName = category == null ? null : category.getCategoryName();
        }
        int safeStock = equipment == null || equipment.getSafeStock() == null ? 0 : equipment.getSafeStock();
        return new StockVO(stock.getEquipmentId(),
                equipment == null ? null : equipment.getEquipmentCode(),
                equipment == null ? null : equipment.getEquipmentName(),
                categoryName,
                equipment == null ? null : equipment.getUnit(),
                stock.getWarehouseId(),
                warehouse == null ? null : warehouse.getWarehouseName(),
                stock.getQuantity(),
                stock.getLockedQuantity(),
                safeStock,
                stock.getQuantity() <= safeStock);
    }

    private StockRecordVO toRecordVo(StockRecord record) {
        Equipment equipment = equipmentMapper.selectById(record.getEquipmentId());
        Warehouse warehouse = warehouseMapper.selectById(record.getWarehouseId());
        return new StockRecordVO(record.getId(),
                equipment == null ? null : equipment.getEquipmentCode(),
                equipment == null ? null : equipment.getEquipmentName(),
                warehouse == null ? null : warehouse.getWarehouseName(),
                record.getChangeType(), record.getChangeQuantity(),
                record.getBeforeQuantity(), record.getAfterQuantity(),
                record.getRefOrderNo(), record.getRemark(), record.getCreateTime());
    }
}
