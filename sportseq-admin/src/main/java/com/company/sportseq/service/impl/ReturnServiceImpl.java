package com.company.sportseq.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.sportseq.common.exception.BizException;
import com.company.sportseq.common.exception.ErrorCode;
import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.common.utils.OrderNoGenerator;
import com.company.sportseq.dto.ReturnItemDTO;
import com.company.sportseq.dto.ReturnOrderDTO;
import com.company.sportseq.entity.BorrowItem;
import com.company.sportseq.entity.BorrowOrder;
import com.company.sportseq.entity.Equipment;
import com.company.sportseq.entity.ReturnItem;
import com.company.sportseq.entity.ReturnOrder;
import com.company.sportseq.entity.StockRecord;
import com.company.sportseq.entity.SysUser;
import com.company.sportseq.entity.Warehouse;
import com.company.sportseq.mapper.BorrowItemMapper;
import com.company.sportseq.mapper.BorrowOrderMapper;
import com.company.sportseq.mapper.EquipmentMapper;
import com.company.sportseq.mapper.EquipmentStockMapper;
import com.company.sportseq.mapper.ReturnItemMapper;
import com.company.sportseq.mapper.ReturnOrderMapper;
import com.company.sportseq.mapper.StockRecordMapper;
import com.company.sportseq.mapper.SysUserMapper;
import com.company.sportseq.mapper.WarehouseMapper;
import com.company.sportseq.security.SecurityUtils;
import com.company.sportseq.service.ReturnService;
import com.company.sportseq.service.SysConfigService;
import com.company.sportseq.vo.ReturnItemVO;
import com.company.sportseq.vo.ReturnOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReturnServiceImpl implements ReturnService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_CONFIRMED = 1;
    private static final int STATUS_REJECTED = 2;

    private final ReturnOrderMapper returnOrderMapper;
    private final ReturnItemMapper returnItemMapper;
    private final BorrowOrderMapper borrowOrderMapper;
    private final BorrowItemMapper borrowItemMapper;
    private final EquipmentStockMapper stockMapper;
    private final StockRecordMapper recordMapper;
    private final EquipmentMapper equipmentMapper;
    private final WarehouseMapper warehouseMapper;
    private final SysUserMapper userMapper;
    private final SysConfigService configService;
    private final StringRedisTemplate redisTemplate;

    @Override
    public PageResult<ReturnOrderVO> page(long current, long size, String orderNo, Integer status) {
        Page<ReturnOrder> page = returnOrderMapper.selectPage(new Page<>(current, size),
                Wrappers.<ReturnOrder>lambdaQuery()
                        .like(StrUtil.isNotBlank(orderNo), ReturnOrder::getOrderNo, orderNo)
                        .eq(status != null, ReturnOrder::getStatus, status)
                        .orderByDesc(ReturnOrder::getCreateTime));
        List<ReturnOrderVO> records = page.getRecords().stream().map(this::toVo).toList();
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    public ReturnOrderVO detail(Long id) {
        ReturnOrder order = getOrder(id);
        return toVo(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(ReturnOrderDTO dto) {
        BorrowOrder borrowOrder = borrowOrderMapper.selectById(dto.getBorrowOrderId());
        if (borrowOrder == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "借用单不存在");
        }
        if (borrowOrder.getStatus() != 2 && borrowOrder.getStatus() != 3 && borrowOrder.getStatus() != 4) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR, "当前借用单状态不允许归还");
        }
        validateReturnQuantities(dto);
        Long userId = SecurityUtils.getUserId();
        ReturnItemDTO first = dto.getItems().get(0);
        BorrowItem firstBorrowItem = borrowItemMapper.selectById(first.getBorrowItemId());
        if (firstBorrowItem == null || !firstBorrowItem.getBorrowId().equals(dto.getBorrowOrderId())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "归还明细不属于该借用单");
        }
        ReturnOrder order = new ReturnOrder();
        order.setOrderNo(OrderNoGenerator.generate("GH", redisTemplate));
        order.setBorrowOrderId(dto.getBorrowOrderId());
        order.setUserId(borrowOrder.getUserId());
        order.setWarehouseId(firstBorrowItem.getWarehouseId());
        order.setRemark(dto.getRemark());
        order.setStatus(STATUS_PENDING);
        int total = first.getQuantity();
        returnOrderMapper.insert(order);
        insertReturnItem(order.getId(), first, firstBorrowItem);
        for (ReturnItemDTO itemDto : dto.getItems()) {
            if (itemDto == first) {
                continue;
            }
            BorrowItem borrowItem = borrowItemMapper.selectById(itemDto.getBorrowItemId());
            if (borrowItem == null || !borrowItem.getBorrowId().equals(dto.getBorrowOrderId())) {
                throw new BizException(ErrorCode.PARAM_ERROR, "归还明细不属于该借用单");
            }
            total += itemDto.getQuantity();
            insertReturnItem(order.getId(), itemDto, borrowItem);
        }
        order.setTotalQuantity(total);
        returnOrderMapper.updateById(order);
    }

    private void insertReturnItem(Long returnId, ReturnItemDTO itemDto, BorrowItem borrowItem) {
        int unreturned = borrowItem.getQuantity() - borrowItem.getReturnedQuantity();
        if (itemDto.getQuantity() > unreturned) {
            throw new BizException(ErrorCode.PARAM_ERROR, "归还数量超过未还数量");
        }
        ReturnItem item = new ReturnItem();
        item.setReturnId(returnId);
        item.setBorrowItemId(borrowItem.getId());
        item.setEquipmentId(borrowItem.getEquipmentId());
        item.setQuantity(itemDto.getQuantity());
        item.setConditionStatus(itemDto.getConditionStatus());
        item.setDamageDesc(itemDto.getDamageDesc());
        item.setIsOverdue(0);
        item.setOverdueDays(0);
        item.setPenaltyAmount(itemDto.getPenaltyAmount());
        returnItemMapper.insert(item);
    }

    /**
     * 归还登记前置校验：同一借用明细多条归还行合并校验，防止超量登记。
     */
    private void validateReturnQuantities(ReturnOrderDTO dto) {
        Map<Long, Integer> quantityByItem = new HashMap<>();
        for (ReturnItemDTO itemDto : dto.getItems()) {
            quantityByItem.merge(itemDto.getBorrowItemId(), itemDto.getQuantity(), Integer::sum);
        }
        for (Map.Entry<Long, Integer> entry : quantityByItem.entrySet()) {
            BorrowItem borrowItem = borrowItemMapper.selectById(entry.getKey());
            if (borrowItem == null || !borrowItem.getBorrowId().equals(dto.getBorrowOrderId())) {
                throw new BizException(ErrorCode.PARAM_ERROR, "归还明细不属于该借用单");
            }
            int unreturned = borrowItem.getQuantity() - borrowItem.getReturnedQuantity();
            if (entry.getValue() > unreturned) {
                throw new BizException(ErrorCode.PARAM_ERROR, "归还数量超过未还数量");
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id) {
        ReturnOrder order = getOrder(id);
        if (order.getStatus() != STATUS_PENDING) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR, "仅待确认状态可验收");
        }
        Long userId = SecurityUtils.getUserId();
        String penaltyStr = configService.getValueByKey("borrow.overdue.penalty");
        BigDecimal penaltyPerDay = new BigDecimal(penaltyStr == null ? "5" : penaltyStr);
        List<ReturnItem> items = returnItemMapper.selectList(
                Wrappers.<ReturnItem>lambdaQuery().eq(ReturnItem::getReturnId, order.getId()));
        for (ReturnItem item : items) {
            BorrowItem borrowItem = borrowItemMapper.selectForUpdate(item.getBorrowItemId());
            if (borrowItem == null) {
                throw new BizException(ErrorCode.PARAM_ERROR, "借用明细不存在");
            }
            int remaining = borrowItem.getQuantity() - borrowItem.getReturnedQuantity();
            if (item.getQuantity() > remaining) {
                throw new BizException(ErrorCode.PARAM_ERROR,
                        "归还数量超过未还数量（未还 " + remaining + "）");
            }
            int before = getQuantity(borrowItem.getEquipmentId(), order.getWarehouseId());
            stockMapper.addQuantity(borrowItem.getEquipmentId(), order.getWarehouseId(),
                    item.getQuantity(), userId);
            StockRecord record = new StockRecord();
            record.setEquipmentId(borrowItem.getEquipmentId());
            record.setWarehouseId(order.getWarehouseId());
            record.setChangeType(3);
            record.setChangeQuantity(item.getQuantity());
            record.setBeforeQuantity(before);
            record.setAfterQuantity(before + item.getQuantity());
            record.setRefOrderType("RETURN");
            record.setRefOrderNo(order.getOrderNo());
            record.setCreateBy(userId);
            record.setCreateTime(LocalDateTime.now());
            recordMapper.insert(record);

            int overdueDays = 0;
            int isOverdue = 0;
            if (borrowItem.getExpectedReturnDate() != null
                    && LocalDate.now().isAfter(borrowItem.getExpectedReturnDate())) {
                overdueDays = (int) ChronoUnit.DAYS.between(borrowItem.getExpectedReturnDate(), LocalDate.now());
                isOverdue = 1;
            }
            boolean manualPenalty = item.getPenaltyAmount() != null
                    && item.getPenaltyAmount().compareTo(BigDecimal.ZERO) > 0;
            BigDecimal penalty = manualPenalty
                    ? item.getPenaltyAmount()
                    : penaltyPerDay.multiply(BigDecimal.valueOf(overdueDays));

            BorrowItem borrowUpdate = new BorrowItem();
            borrowUpdate.setId(borrowItem.getId());
            borrowUpdate.setReturnedQuantity(borrowItem.getReturnedQuantity() + item.getQuantity());
            borrowUpdate.setOverdueFlag(isOverdue);
            borrowUpdate.setOverdueDays(overdueDays);
            borrowUpdate.setActualReturnDate(LocalDate.now());
            int unreturned = borrowItem.getQuantity() - borrowItem.getReturnedQuantity() - item.getQuantity();
            borrowUpdate.setStatus(unreturned == 0 ? 3 : 2);
            borrowItemMapper.updateById(borrowUpdate);

            ReturnItem itemUpdate = new ReturnItem();
            itemUpdate.setId(item.getId());
            itemUpdate.setIsOverdue(isOverdue);
            itemUpdate.setOverdueDays(overdueDays);
            itemUpdate.setPenaltyAmount(penalty);
            returnItemMapper.updateById(itemUpdate);
        }
        List<BorrowItem> remaining = borrowItemMapper.selectList(
                Wrappers.<BorrowItem>lambdaQuery().eq(BorrowItem::getBorrowId, order.getBorrowOrderId()));
        boolean allReturned = remaining.stream().allMatch(i ->
                i.getQuantity().equals(i.getReturnedQuantity()));
        BorrowOrder borrowUpdate = new BorrowOrder();
        borrowUpdate.setId(order.getBorrowOrderId());
        borrowUpdate.setStatus(allReturned ? 4 : 3);
        borrowOrderMapper.updateById(borrowUpdate);

        ReturnOrder orderUpdate = new ReturnOrder();
        orderUpdate.setId(order.getId());
        orderUpdate.setStatus(STATUS_CONFIRMED);
        orderUpdate.setConfirmBy(userId);
        orderUpdate.setConfirmTime(LocalDateTime.now());
        returnOrderMapper.updateById(orderUpdate);
    }

    @Override
    public void reject(Long id) {
        ReturnOrder order = getOrder(id);
        if (order.getStatus() != STATUS_PENDING) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR, "仅待确认状态可驳回");
        }
        ReturnOrder update = new ReturnOrder();
        update.setId(order.getId());
        update.setStatus(STATUS_REJECTED);
        returnOrderMapper.updateById(update);
    }

    private int getQuantity(Long equipmentId, Long warehouseId) {
        var stock = stockMapper.selectForUpdate(equipmentId, warehouseId);
        return stock == null ? 0 : stock.getQuantity();
    }

    private ReturnOrder getOrder(Long id) {
        ReturnOrder order = returnOrderMapper.selectById(id);
        if (order == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "归还单不存在");
        }
        return order;
    }

    private ReturnOrderVO toVo(ReturnOrder order) {
        BorrowOrder borrowOrder = borrowOrderMapper.selectById(order.getBorrowOrderId());
        SysUser user = userMapper.selectById(order.getUserId());
        Warehouse warehouse = warehouseMapper.selectById(order.getWarehouseId());
        List<ReturnItemVO> items = returnItemMapper.selectList(
                        Wrappers.<ReturnItem>lambdaQuery().eq(ReturnItem::getReturnId, order.getId()))
                .stream().map(item -> {
                    Equipment equipment = equipmentMapper.selectById(item.getEquipmentId());
                    return new ReturnItemVO(item.getId(), item.getBorrowItemId(),
                            equipment == null ? null : equipment.getEquipmentCode(),
                            equipment == null ? null : equipment.getEquipmentName(),
                            item.getQuantity(), item.getConditionStatus(), item.getDamageDesc(),
                            item.getIsOverdue(), item.getOverdueDays(), item.getPenaltyAmount());
                }).toList();
        return new ReturnOrderVO(order.getId(), order.getOrderNo(), order.getBorrowOrderId(),
                borrowOrder == null ? null : borrowOrder.getOrderNo(),
                user == null ? null : user.getUsername(),
                order.getWarehouseId(), warehouse == null ? null : warehouse.getWarehouseName(),
                order.getTotalQuantity(), order.getReturnType(), order.getStatus(),
                order.getConfirmRemark(), order.getRemark(), order.getCreateTime(), items);
    }
}
