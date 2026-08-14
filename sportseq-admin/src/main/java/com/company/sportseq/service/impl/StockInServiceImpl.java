package com.company.sportseq.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.sportseq.common.cache.CacheService;
import com.company.sportseq.common.constant.CacheConstants;
import com.company.sportseq.common.exception.BizException;
import com.company.sportseq.common.exception.ErrorCode;
import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.common.utils.OrderNoGenerator;
import com.company.sportseq.dto.StockInAuditDTO;
import com.company.sportseq.dto.StockInItemDTO;
import com.company.sportseq.dto.StockInOrderDTO;
import com.company.sportseq.entity.Equipment;
import com.company.sportseq.entity.EquipmentStock;
import com.company.sportseq.entity.StockInItem;
import com.company.sportseq.entity.StockInOrder;
import com.company.sportseq.entity.StockRecord;
import com.company.sportseq.entity.Warehouse;
import com.company.sportseq.mapper.EquipmentMapper;
import com.company.sportseq.mapper.EquipmentStockMapper;
import com.company.sportseq.mapper.StockInItemMapper;
import com.company.sportseq.mapper.StockInOrderMapper;
import com.company.sportseq.mapper.StockRecordMapper;
import com.company.sportseq.mapper.WarehouseMapper;
import com.company.sportseq.security.SecurityUtils;
import com.company.sportseq.service.StockInService;
import com.company.sportseq.vo.StockInItemVO;
import com.company.sportseq.vo.StockInVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockInServiceImpl implements StockInService {

    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PENDING = 1;
    private static final int STATUS_APPROVED = 2;
    private static final int STATUS_RECEIVED = 3;
    private static final int STATUS_REJECTED = 4;
    private static final int STATUS_CANCELED = 5;

    private final StockInOrderMapper orderMapper;
    private final StockInItemMapper itemMapper;
    private final EquipmentStockMapper stockMapper;
    private final StockRecordMapper recordMapper;
    private final EquipmentMapper equipmentMapper;
    private final WarehouseMapper warehouseMapper;
    private final StringRedisTemplate redisTemplate;
    private final CacheService cacheService;

    @Override
    public PageResult<StockInVO> page(long current, long size, String orderNo, Integer status) {
        Page<StockInOrder> page = orderMapper.selectPage(new Page<>(current, size),
                Wrappers.<StockInOrder>lambdaQuery()
                        .like(StrUtil.isNotBlank(orderNo), StockInOrder::getOrderNo, orderNo)
                        .eq(status != null, StockInOrder::getStatus, status)
                        .orderByDesc(StockInOrder::getCreateTime));
        List<StockInVO> records = page.getRecords().stream().map(this::toVoWithoutItems).toList();
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    public StockInVO detail(Long id) {
        StockInOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "入库单不存在");
        }
        return toVo(order, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(StockInOrderDTO dto) {
        validateWarehouse(dto.getWarehouseId());
        StockInOrder order = new StockInOrder();
        order.setOrderNo(OrderNoGenerator.generate("RK", redisTemplate));
        order.setWarehouseId(dto.getWarehouseId());
        order.setSupplier(dto.getSupplier());
        order.setInType(dto.getInType());
        order.setStatus(STATUS_DRAFT);
        int totalQuantity = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (StockInItemDTO itemDto : dto.getItems()) {
            Equipment equipment = equipmentMapper.selectById(itemDto.getEquipmentId());
            if (equipment == null) {
                throw new BizException(ErrorCode.PARAM_ERROR, "器材不存在: " + itemDto.getEquipmentId());
            }
            totalQuantity += itemDto.getQuantity();
            BigDecimal amount = itemDto.getUnitPrice() == null
                    ? BigDecimal.ZERO
                    : itemDto.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalAmount = totalAmount.add(amount);
        }
        order.setTotalQuantity(totalQuantity);
        order.setTotalAmount(totalAmount);
        orderMapper.insert(order);
        for (StockInItemDTO itemDto : dto.getItems()) {
            StockInItem item = new StockInItem();
            item.setOrderId(order.getId());
            item.setEquipmentId(itemDto.getEquipmentId());
            item.setQuantity(itemDto.getQuantity());
            item.setUnitPrice(itemDto.getUnitPrice());
            item.setAmount(itemDto.getUnitPrice() == null
                    ? null
                    : itemDto.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity())));
            item.setRemark(itemDto.getRemark());
            itemMapper.insert(item);
        }
        cacheService.evictAfterCommit(this::evictDashboardSummary);
    }

    @Override
    public void submit(Long id) {
        StockInOrder order = getOrder(id);
        checkStatus(order, STATUS_DRAFT, "草稿状态才能提交");
        updateStatus(order, STATUS_PENDING, null);
    }

    @Override
    public void audit(StockInAuditDTO dto) {
        StockInOrder order = getOrder(dto.getOrderId());
        checkStatus(order, STATUS_PENDING, "待审核状态才能审核");
        Long userId = SecurityUtils.getUserId();
        StockInOrder update = new StockInOrder();
        update.setId(order.getId());
        update.setStatus(Boolean.TRUE.equals(dto.getPass()) ? STATUS_APPROVED : STATUS_REJECTED);
        update.setAuditBy(userId);
        update.setAuditTime(LocalDateTime.now());
        update.setAuditRemark(dto.getRemark());
        orderMapper.updateById(update);
        if (!Boolean.TRUE.equals(dto.getPass())) {
            cacheService.evictAfterCommit(this::evictDashboardSummary);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receive(Long id) {
        evictStockCaches();
        StockInOrder order = getOrder(id);
        checkStatus(order, STATUS_APPROVED, "审核通过后才能验收");
        Long userId = SecurityUtils.getUserId();
        List<StockInItem> items = itemMapper.selectList(
                Wrappers.<StockInItem>lambdaQuery().eq(StockInItem::getOrderId, order.getId()));
        for (StockInItem item : items) {
            EquipmentStock stock = stockMapper.selectForUpdate(item.getEquipmentId(), order.getWarehouseId());
            int before = stock == null ? 0 : stock.getQuantity();
            if (stock == null) {
                stock = new EquipmentStock();
                stock.setEquipmentId(item.getEquipmentId());
                stock.setWarehouseId(order.getWarehouseId());
                stock.setQuantity(0);
                stock.setLockedQuantity(0);
                stock.setVersion(0);
                stockMapper.insert(stock);
            }
            stockMapper.addQuantity(item.getEquipmentId(), order.getWarehouseId(), item.getQuantity(), userId);
            StockRecord record = new StockRecord();
            record.setEquipmentId(item.getEquipmentId());
            record.setWarehouseId(order.getWarehouseId());
            record.setChangeType(1);
            record.setChangeQuantity(item.getQuantity());
            record.setBeforeQuantity(before);
            record.setAfterQuantity(before + item.getQuantity());
            record.setRefOrderType("STOCK_IN");
            record.setRefOrderNo(order.getOrderNo());
            record.setCreateBy(userId);
            record.setCreateTime(LocalDateTime.now());
            recordMapper.insert(record);
        }
        StockInOrder update = new StockInOrder();
        update.setId(order.getId());
        update.setStatus(STATUS_RECEIVED);
        update.setReceiveBy(userId);
        update.setReceiveTime(LocalDateTime.now());
        orderMapper.updateById(update);
        cacheService.evictAfterCommit(this::evictStockCaches);
    }

    @Override
    public void cancel(Long id) {
        StockInOrder order = getOrder(id);
        if (order.getStatus() != STATUS_DRAFT && order.getStatus() != STATUS_PENDING
                && order.getStatus() != STATUS_REJECTED) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR, "当前状态不允许作废");
        }
        updateStatus(order, STATUS_CANCELED, null);
        cacheService.evictAfterCommit(this::evictDashboardSummary);
    }

    @Override
    public void remove(Long id) {
        StockInOrder order = getOrder(id);
        checkStatus(order, STATUS_DRAFT, "仅草稿状态可删除");
        itemMapper.delete(Wrappers.<StockInItem>lambdaQuery().eq(StockInItem::getOrderId, id));
        orderMapper.deleteById(id);
        cacheService.evictAfterCommit(this::evictDashboardSummary);
    }

    private StockInOrder getOrder(Long id) {
        StockInOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "入库单不存在");
        }
        return order;
    }

    private void evictStockCaches() {
        cacheService.evictByPattern(CacheConstants.STOCK_PAGE_PATTERN);
        cacheService.evict(CacheConstants.DASHBOARD_SUMMARY_KEY);
    }

    private void evictDashboardSummary() {
        cacheService.evict(CacheConstants.DASHBOARD_SUMMARY_KEY);
    }

    private void checkStatus(StockInOrder order, int expected, String message) {
        if (order.getStatus() != expected) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR, message);
        }
    }

    private void updateStatus(StockInOrder order, int status, String remark) {
        StockInOrder update = new StockInOrder();
        update.setId(order.getId());
        update.setStatus(status);
        update.setRemark(remark);
        orderMapper.updateById(update);
    }

    private void validateWarehouse(Long warehouseId) {
        Warehouse warehouse = warehouseMapper.selectById(warehouseId);
        if (warehouse == null || (warehouse.getStatus() != null && warehouse.getStatus() == 0)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "仓库不存在或已停用");
        }
    }

    private StockInVO toVoWithoutItems(StockInOrder order) {
        return toVo(order, false);
    }

    private StockInVO toVo(StockInOrder order, boolean withItems) {
        Warehouse warehouse = warehouseMapper.selectById(order.getWarehouseId());
        List<StockInItemVO> items = null;
        if (withItems) {
            items = itemMapper.selectList(
                            Wrappers.<StockInItem>lambdaQuery().eq(StockInItem::getOrderId, order.getId()))
                    .stream().map(item -> {
                        Equipment equipment = equipmentMapper.selectById(item.getEquipmentId());
                        return new StockInItemVO(item.getId(), item.getEquipmentId(),
                                equipment == null ? null : equipment.getEquipmentCode(),
                                equipment == null ? null : equipment.getEquipmentName(),
                                equipment == null ? null : equipment.getUnit(),
                                item.getQuantity(), item.getUnitPrice(), item.getAmount(), item.getRemark());
                    }).toList();
        }
        return new StockInVO(order.getId(), order.getOrderNo(), order.getWarehouseId(),
                warehouse == null ? null : warehouse.getWarehouseName(), order.getSupplier(),
                order.getInType(), order.getTotalQuantity(), order.getTotalAmount(), order.getStatus(),
                order.getAuditBy(), order.getAuditTime(), order.getAuditRemark(),
                order.getReceiveBy(), order.getReceiveTime(), order.getRemark(), order.getCreateTime(), items);
    }
}
