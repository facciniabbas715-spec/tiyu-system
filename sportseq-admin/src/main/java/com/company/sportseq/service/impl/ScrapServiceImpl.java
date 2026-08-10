package com.company.sportseq.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.sportseq.common.exception.BizException;
import com.company.sportseq.common.exception.ErrorCode;
import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.common.utils.OrderNoGenerator;
import com.company.sportseq.dto.ScrapAuditDTO;
import com.company.sportseq.dto.ScrapDisposeDTO;
import com.company.sportseq.dto.ScrapItemDTO;
import com.company.sportseq.dto.ScrapOrderDTO;
import com.company.sportseq.entity.Equipment;
import com.company.sportseq.entity.ScrapItem;
import com.company.sportseq.entity.ScrapOrder;
import com.company.sportseq.entity.StockRecord;
import com.company.sportseq.entity.Warehouse;
import com.company.sportseq.mapper.EquipmentMapper;
import com.company.sportseq.mapper.EquipmentStockMapper;
import com.company.sportseq.mapper.ScrapItemMapper;
import com.company.sportseq.mapper.ScrapOrderMapper;
import com.company.sportseq.mapper.StockRecordMapper;
import com.company.sportseq.mapper.WarehouseMapper;
import com.company.sportseq.security.SecurityUtils;
import com.company.sportseq.service.ScrapService;
import com.company.sportseq.vo.ScrapItemVO;
import com.company.sportseq.vo.ScrapOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScrapServiceImpl implements ScrapService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_DISPOSED = 2;
    private static final int STATUS_REJECTED = 3;
    private static final int STATUS_CANCELED = 4;

    private final ScrapOrderMapper orderMapper;
    private final ScrapItemMapper itemMapper;
    private final EquipmentStockMapper stockMapper;
    private final StockRecordMapper recordMapper;
    private final EquipmentMapper equipmentMapper;
    private final WarehouseMapper warehouseMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public PageResult<ScrapOrderVO> page(long current, long size, String orderNo, Integer status) {
        Page<ScrapOrder> page = orderMapper.selectPage(new Page<>(current, size),
                Wrappers.<ScrapOrder>lambdaQuery()
                        .like(StrUtil.isNotBlank(orderNo), ScrapOrder::getOrderNo, orderNo)
                        .eq(status != null, ScrapOrder::getStatus, status)
                        .orderByDesc(ScrapOrder::getCreateTime));
        List<ScrapOrderVO> records = buildVos(page.getRecords());
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    public ScrapOrderVO detail(Long id) {
        return toVo(getOrder(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(ScrapOrderDTO dto) {
        Warehouse warehouse = warehouseMapper.selectById(dto.getWarehouseId());
        if (warehouse == null || (warehouse.getStatus() != null && warehouse.getStatus() == 0)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "仓库不存在或已停用");
        }
        ScrapOrder order = new ScrapOrder();
        order.setOrderNo(OrderNoGenerator.generate("BF", redisTemplate));
        order.setWarehouseId(dto.getWarehouseId());
        order.setScrapType(dto.getScrapType());
        order.setStatus(STATUS_PENDING);
        order.setRemark(dto.getRemark());
        int totalQuantity = 0;
        BigDecimal totalLoss = BigDecimal.ZERO;
        for (ScrapItemDTO itemDto : dto.getItems()) {
            Equipment equipment = equipmentMapper.selectById(itemDto.getEquipmentId());
            if (equipment == null) {
                throw new BizException(ErrorCode.PARAM_ERROR, "器材不存在");
            }
            totalQuantity += itemDto.getQuantity();
            BigDecimal loss = equipment.getPurchasePrice() == null
                    ? BigDecimal.ZERO
                    : equipment.getPurchasePrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalLoss = totalLoss.add(loss);
        }
        order.setTotalQuantity(totalQuantity);
        order.setTotalLossAmount(totalLoss);
        orderMapper.insert(order);
        for (ScrapItemDTO itemDto : dto.getItems()) {
            Equipment equipment = equipmentMapper.selectById(itemDto.getEquipmentId());
            ScrapItem item = new ScrapItem();
            item.setScrapId(order.getId());
            item.setEquipmentId(itemDto.getEquipmentId());
            item.setQuantity(itemDto.getQuantity());
            item.setScrapReason(itemDto.getScrapReason());
            item.setLossAmount(equipment.getPurchasePrice() == null
                    ? BigDecimal.ZERO
                    : equipment.getPurchasePrice().multiply(BigDecimal.valueOf(itemDto.getQuantity())));
            itemMapper.insert(item);
        }
    }

    @Override
    public void audit(ScrapAuditDTO dto) {
        ScrapOrder order = getOrder(dto.getOrderId());
        checkStatus(order, STATUS_PENDING, "待审核状态才能审核");
        ScrapOrder update = new ScrapOrder();
        update.setId(order.getId());
        update.setStatus(Boolean.TRUE.equals(dto.getPass()) ? STATUS_APPROVED : STATUS_REJECTED);
        update.setAuditBy(SecurityUtils.getUserId());
        update.setAuditTime(LocalDateTime.now());
        update.setAuditRemark(dto.getRemark());
        orderMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dispose(ScrapDisposeDTO dto) {
        ScrapOrder order = getOrder(dto.getOrderId());
        checkStatus(order, STATUS_APPROVED, "审核通过后才能处置");
        Long userId = SecurityUtils.getUserId();
        List<ScrapItem> items = itemMapper.selectList(
                Wrappers.<ScrapItem>lambdaQuery().eq(ScrapItem::getScrapId, order.getId()));
        for (ScrapItem item : items) {
            var stock = stockMapper.selectForUpdate(item.getEquipmentId(), order.getWarehouseId());
            int before = stock == null ? 0 : stock.getQuantity();
            int affected = stockMapper.subtractAvailableQuantity(item.getEquipmentId(), order.getWarehouseId(),
                    item.getQuantity(), userId);
            if (affected == 0) {
                throw new BizException(ErrorCode.STOCK_NOT_ENOUGH, "可用库存不足，无法完成报废处置");
            }
            StockRecord record = new StockRecord();
            record.setEquipmentId(item.getEquipmentId());
            record.setWarehouseId(order.getWarehouseId());
            record.setChangeType(4);
            record.setChangeQuantity(-item.getQuantity());
            record.setBeforeQuantity(before);
            record.setAfterQuantity(before - item.getQuantity());
            record.setRefOrderType("SCRAP");
            record.setRefOrderNo(order.getOrderNo());
            record.setCreateBy(userId);
            record.setCreateTime(LocalDateTime.now());
            recordMapper.insert(record);
        }
        ScrapOrder update = new ScrapOrder();
        update.setId(order.getId());
        update.setStatus(STATUS_DISPOSED);
        update.setDisposeBy(userId);
        update.setDisposeTime(LocalDateTime.now());
        update.setDisposeMethod(dto.getDisposeMethod());
        update.setRemark(dto.getRemark());
        orderMapper.updateById(update);
    }

    @Override
    public void cancel(Long id) {
        ScrapOrder order = getOrder(id);
        if (order.getStatus() != STATUS_PENDING && order.getStatus() != STATUS_REJECTED) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR, "当前状态不允许作废");
        }
        ScrapOrder update = new ScrapOrder();
        update.setId(order.getId());
        update.setStatus(STATUS_CANCELED);
        orderMapper.updateById(update);
    }

    private ScrapOrder getOrder(Long id) {
        ScrapOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "报废单不存在");
        }
        return order;
    }

    private void checkStatus(ScrapOrder order, int expected, String message) {
        if (order.getStatus() != expected) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR, message);
        }
    }

    private ScrapOrderVO toVo(ScrapOrder order) {
        List<ScrapItem> items = itemMapper.selectList(
                Wrappers.<ScrapItem>lambdaQuery().eq(ScrapItem::getScrapId, order.getId()));
        List<Long> equipmentIds = items.stream()
                .map(ScrapItem::getEquipmentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Equipment> equipmentMap = equipmentIds.isEmpty() ? Map.of()
                : equipmentMapper.selectBatchIds(equipmentIds).stream()
                        .collect(Collectors.toMap(Equipment::getId, e -> e, (a, b) -> a));
        Warehouse warehouse = warehouseMapper.selectById(order.getWarehouseId());
        Map<Long, Warehouse> warehouseMap = warehouse == null ? Map.of()
                : Map.of(order.getWarehouseId(), warehouse);
        return toVo(order, warehouseMap, items, equipmentMap);
    }

    private List<ScrapOrderVO> buildVos(List<ScrapOrder> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }
        List<Long> warehouseIds = orders.stream()
                .map(ScrapOrder::getWarehouseId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Warehouse> warehouseMap = warehouseIds.isEmpty() ? Map.of()
                : warehouseMapper.selectBatchIds(warehouseIds).stream()
                        .collect(Collectors.toMap(Warehouse::getId, w -> w, (a, b) -> a));
        List<Long> scrapIds = orders.stream().map(ScrapOrder::getId).toList();
        List<ScrapItem> allItems = itemMapper.selectList(
                Wrappers.<ScrapItem>lambdaQuery().in(ScrapItem::getScrapId, scrapIds));
        Map<Long, List<ScrapItem>> itemsByScrap = allItems.stream()
                .collect(Collectors.groupingBy(ScrapItem::getScrapId));
        List<Long> equipmentIds = allItems.stream()
                .map(ScrapItem::getEquipmentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Equipment> equipmentMap = equipmentIds.isEmpty() ? Map.of()
                : equipmentMapper.selectBatchIds(equipmentIds).stream()
                        .collect(Collectors.toMap(Equipment::getId, e -> e, (a, b) -> a));
        return orders.stream()
                .map(order -> toVo(order, warehouseMap,
                        itemsByScrap.getOrDefault(order.getId(), List.of()),
                        equipmentMap))
                .toList();
    }

    private ScrapOrderVO toVo(ScrapOrder order, Map<Long, Warehouse> warehouseMap,
                              List<ScrapItem> items, Map<Long, Equipment> equipmentMap) {
        Warehouse warehouse = warehouseMap.get(order.getWarehouseId());
        List<ScrapItemVO> itemVos = items.stream().map(item -> {
            Equipment equipment = equipmentMap.get(item.getEquipmentId());
            return new ScrapItemVO(item.getId(), item.getEquipmentId(),
                    equipment == null ? null : equipment.getEquipmentCode(),
                    equipment == null ? null : equipment.getEquipmentName(),
                    equipment == null ? null : equipment.getUnit(),
                    item.getQuantity(), item.getScrapReason(), item.getLossAmount());
        }).toList();
        return new ScrapOrderVO(order.getId(), order.getOrderNo(), order.getWarehouseId(),
                warehouse == null ? null : warehouse.getWarehouseName(), order.getScrapType(),
                order.getTotalQuantity(), order.getTotalLossAmount(), order.getStatus(),
                order.getAuditRemark(), order.getDisposeMethod(), order.getRemark(),
                order.getCreateTime(), itemVos);
    }
}
