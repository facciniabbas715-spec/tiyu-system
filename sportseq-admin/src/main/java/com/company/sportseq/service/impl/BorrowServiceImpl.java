package com.company.sportseq.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.sportseq.common.exception.BizException;
import com.company.sportseq.common.exception.ErrorCode;
import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.common.utils.OrderNoGenerator;
import com.company.sportseq.dto.BorrowAuditDTO;
import com.company.sportseq.dto.BorrowExtendDTO;
import com.company.sportseq.dto.BorrowItemDTO;
import com.company.sportseq.dto.BorrowOrderDTO;
import com.company.sportseq.entity.BorrowItem;
import com.company.sportseq.entity.BorrowOrder;
import com.company.sportseq.entity.Equipment;
import com.company.sportseq.entity.SysUser;
import com.company.sportseq.entity.Warehouse;
import com.company.sportseq.mapper.BorrowItemMapper;
import com.company.sportseq.mapper.BorrowOrderMapper;
import com.company.sportseq.mapper.EquipmentMapper;
import com.company.sportseq.mapper.EquipmentStockMapper;
import com.company.sportseq.mapper.SysUserMapper;
import com.company.sportseq.mapper.WarehouseMapper;
import com.company.sportseq.mapper.StockRecordMapper;
import com.company.sportseq.entity.StockRecord;
import com.company.sportseq.security.SecurityUtils;
import com.company.sportseq.service.BorrowService;
import com.company.sportseq.service.DataScopeService;
import com.company.sportseq.service.SysConfigService;
import com.company.sportseq.vo.BorrowItemVO;
import com.company.sportseq.vo.BorrowOrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BorrowServiceImpl implements BorrowService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_APPROVED = 1;
    private static final int STATUS_BORROWED = 2;
    private static final int STATUS_PARTIAL = 3;
    private static final int STATUS_RETURNED = 4;
    private static final int STATUS_REJECTED = 5;
    private static final int STATUS_CANCELED = 6;

    private final BorrowOrderMapper orderMapper;
    private final BorrowItemMapper itemMapper;
    private final EquipmentStockMapper stockMapper;
    private final EquipmentMapper equipmentMapper;
    private final WarehouseMapper warehouseMapper;
    private final SysUserMapper userMapper;
    private final StockRecordMapper recordMapper;
    private final SysConfigService configService;
    private final StringRedisTemplate redisTemplate;
    private final DataScopeService dataScopeService;

    @Override
    public PageResult<BorrowOrderVO> page(long current, long size, String orderNo, String username,
                                          Integer status, Long borrowUserId) {
        var wrapper = Wrappers.<BorrowOrder>lambdaQuery()
                .like(StrUtil.isNotBlank(orderNo), BorrowOrder::getOrderNo, orderNo)
                .eq(status != null, BorrowOrder::getStatus, status)
                .eq(borrowUserId != null, BorrowOrder::getUserId, borrowUserId);
        if (StrUtil.isNotBlank(username)) {
            wrapper.apply("user_id IN (SELECT id FROM sys_user WHERE username LIKE CONCAT('%', {0}, '%') " +
                            "OR real_name LIKE CONCAT('%', {1}, '%'))",
                    username, username);
        }
        applyDataScope(wrapper);
        wrapper.orderByDesc(BorrowOrder::getCreateTime);
        Page<BorrowOrder> page = orderMapper.selectPage(new Page<>(current, size), wrapper);
        List<BorrowOrderVO> records = buildVos(page.getRecords());
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    public PageResult<BorrowOrderVO> myPage(long current, long size, Integer status) {
        return page(current, size, null, null, status, SecurityUtils.getUserId());
    }

    @Override
    public BorrowOrderVO detail(Long id) {
        BorrowOrder order = getOrder(id);
        checkDataScope(order);
        return toVo(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(BorrowOrderDTO dto) {
        Long userId = SecurityUtils.getUserId();
        SysUser user = userMapper.selectById(userId);
        BorrowOrder order = new BorrowOrder();
        order.setOrderNo(OrderNoGenerator.generate("JY", redisTemplate));
        order.setUserId(userId);
        order.setDeptId(user == null ? null : user.getDeptId());
        order.setBorrowType(dto.getBorrowType());
        order.setPurpose(dto.getPurpose());
        order.setExpectedReturnDate(dto.getExpectedReturnDate());
        order.setStatus(STATUS_PENDING);
        order.setLocked(1);
        order.setRemark(dto.getRemark());
        int total = dto.getItems().stream().mapToInt(BorrowItemDTO::getQuantity).sum();
        order.setTotalQuantity(total);
        orderMapper.insert(order);

        for (BorrowItemDTO itemDto : dto.getItems()) {
            Equipment equipment = equipmentMapper.selectById(itemDto.getEquipmentId());
            if (equipment == null) {
                throw new BizException(ErrorCode.PARAM_ERROR, "器材不存在: " + itemDto.getEquipmentId());
            }
            int locked = stockMapper.lockQuantity(itemDto.getEquipmentId(), itemDto.getWarehouseId(),
                    itemDto.getQuantity(), userId);
            if (locked == 0) {
                throw new BizException(ErrorCode.STOCK_NOT_ENOUGH,
                        "「" + equipment.getEquipmentName() + "」可用库存不足");
            }
            BorrowItem item = new BorrowItem();
            item.setBorrowId(order.getId());
            item.setEquipmentId(itemDto.getEquipmentId());
            item.setWarehouseId(itemDto.getWarehouseId());
            item.setQuantity(itemDto.getQuantity());
            item.setExpectedReturnDate(dto.getExpectedReturnDate());
            item.setStatus(0);
            item.setOverdueFlag(0);
            item.setOverdueDays(0);
            itemMapper.insert(item);
        }
    }

    @Override
    public void audit(BorrowAuditDTO dto) {
        BorrowOrder order = getOrder(dto.getOrderId());
        checkDataScope(order);
        checkStatus(order, STATUS_PENDING, "待审核状态才能审核");
        Long userId = SecurityUtils.getUserId();
        BorrowOrder update = new BorrowOrder();
        update.setId(order.getId());
        update.setStatus(Boolean.TRUE.equals(dto.getPass()) ? STATUS_APPROVED : STATUS_REJECTED);
        update.setAuditBy(userId);
        update.setAuditTime(LocalDateTime.now());
        update.setAuditRemark(dto.getRemark());
        orderMapper.updateById(update);
        if (!Boolean.TRUE.equals(dto.getPass())) {
            unlockItems(order.getId(), userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void issue(Long id) {
        BorrowOrder order = getOrder(id);
        checkDataScope(order);
        checkStatus(order, STATUS_APPROVED, "审核通过后才能领用");
        Long userId = SecurityUtils.getUserId();
        List<BorrowItem> items = loadItems(order.getId());
        for (BorrowItem item : items) {
            var stock = stockMapper.selectForUpdate(item.getEquipmentId(), item.getWarehouseId());
            if (stock == null || stock.getQuantity() < item.getQuantity()
                    || stock.getLockedQuantity() < item.getQuantity()) {
                throw new BizException(ErrorCode.STOCK_NOT_ENOUGH, "器材库存不足，无法发放");
            }
            int before = stock == null ? 0 : stock.getQuantity();
            int affected = stockMapper.subtractQuantity(item.getEquipmentId(), item.getWarehouseId(),
                    item.getQuantity(), userId);
            if (affected == 0) {
                throw new BizException(ErrorCode.STOCK_NOT_ENOUGH, "器材库存不足，无法发放");
            }
            stockMapper.unlockQuantity(item.getEquipmentId(), item.getWarehouseId(), item.getQuantity(), userId);
            StockRecord record = new StockRecord();
            record.setEquipmentId(item.getEquipmentId());
            record.setWarehouseId(item.getWarehouseId());
            record.setChangeType(2);
            record.setChangeQuantity(-item.getQuantity());
            record.setBeforeQuantity(before);
            record.setAfterQuantity(before - item.getQuantity());
            record.setRefOrderType("BORROW");
            record.setRefOrderNo(order.getOrderNo());
            record.setCreateBy(userId);
            record.setCreateTime(LocalDateTime.now());
            recordMapper.insert(record);
        }
        for (BorrowItem item : items) {
            BorrowItem up = new BorrowItem();
            up.setId(item.getId());
            up.setIssuedQuantity(item.getQuantity());
            up.setStatus(1);
            itemMapper.updateById(up);
        }
        BorrowOrder update = new BorrowOrder();
        update.setId(order.getId());
        update.setStatus(STATUS_BORROWED);
        update.setIssueBy(userId);
        update.setIssueTime(LocalDateTime.now());
        orderMapper.updateById(update);
    }

    @Override
    public void extend(BorrowExtendDTO dto) {
        BorrowOrder order = getOrder(dto.getOrderId());
        checkDataScope(order);
        checkStatus(order, STATUS_BORROWED, "借用中才能续借");
        String limitStr = configService.getValueByKey("borrow.extend.limit");
        int limit = limitStr == null ? 1 : Integer.parseInt(limitStr);
        if (order.getExtendCount() != null && order.getExtendCount() >= limit) {
            throw new BizException(ErrorCode.PARAM_ERROR, "已达续借次数上限");
        }
        LocalDate newDate = order.getExpectedReturnDate().plusDays(dto.getDays());
        BorrowOrder update = new BorrowOrder();
        update.setId(order.getId());
        update.setExpectedReturnDate(newDate);
        update.setExtendCount((order.getExtendCount() == null ? 0 : order.getExtendCount()) + 1);
        orderMapper.updateById(update);
        BorrowItem itemUpdate = new BorrowItem();
        itemUpdate.setExpectedReturnDate(newDate);
        itemMapper.update(itemUpdate, Wrappers.<BorrowItem>lambdaQuery().eq(BorrowItem::getBorrowId, order.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) {
        BorrowOrder order = getOrder(id);
        checkDataScope(order);
        checkStatus(order, STATUS_PENDING, "仅待审核状态可取消");
        unlockItems(order.getId(), SecurityUtils.getUserId());
        BorrowOrder update = new BorrowOrder();
        update.setId(order.getId());
        update.setStatus(STATUS_CANCELED);
        orderMapper.updateById(update);
    }

    private void unlockItems(Long orderId, Long userId) {
        List<BorrowItem> items = loadItems(orderId);
        for (BorrowItem item : items) {
            if (item.getIssuedQuantity() == null || item.getIssuedQuantity() == 0) {
                stockMapper.unlockQuantity(item.getEquipmentId(), item.getWarehouseId(),
                        item.getQuantity(), userId);
            }
        }
    }

    private List<BorrowItem> loadItems(Long orderId) {
        return itemMapper.selectList(Wrappers.<BorrowItem>lambdaQuery()
                .eq(BorrowItem::getBorrowId, orderId));
    }

    private BorrowOrder getOrder(Long id) {
        BorrowOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "借用单不存在");
        }
        return order;
    }

    private void checkStatus(BorrowOrder order, int expected, String message) {
        if (order.getStatus() != expected) {
            throw new BizException(ErrorCode.ORDER_STATUS_ERROR, message);
        }
    }

    private void applyDataScope(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BorrowOrder> wrapper) {
        if (dataScopeService.isFullScope()) {
            return;
        }
        if (dataScopeService.isSelfOnly()) {
            wrapper.eq(BorrowOrder::getUserId, dataScopeService.currentUserId());
            return;
        }
        Set<Long> deptIds = dataScopeService.allowedDeptIds();
        if (deptIds.isEmpty()) {
            wrapper.apply("1 = 0");
        } else {
            wrapper.in(BorrowOrder::getDeptId, deptIds);
        }
    }

    private void checkDataScope(BorrowOrder order) {
        if (dataScopeService.isFullScope()) {
            return;
        }
        if (dataScopeService.isSelfOnly()) {
            if (!dataScopeService.currentUserId().equals(order.getUserId())) {
                throw new AccessDeniedException("无权限访问该借用单");
            }
            return;
        }
        Set<Long> deptIds = dataScopeService.allowedDeptIds();
        if (order.getDeptId() == null || !deptIds.contains(order.getDeptId())) {
            throw new AccessDeniedException("无权限访问该借用单");
        }
    }

    private SysUser fetchUser(Long userId) {
        SysUser user = userMapper.selectById(userId);
        return user == null ? new SysUser() : user;
    }

    private BorrowOrderVO toVo(BorrowOrder order) {
        SysUser user = fetchUser(order.getUserId());
        List<BorrowItem> items = loadItems(order.getId());
        Map<Long, Equipment> equipmentMap = items.stream()
                .map(i -> equipmentMapper.selectById(i.getEquipmentId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Equipment::getId, e -> e, (a, b) -> a));
        Map<Long, Warehouse> warehouseMap = items.stream()
                .map(i -> warehouseMapper.selectById(i.getWarehouseId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Warehouse::getId, w -> w, (a, b) -> a));
        return toVo(order, Map.of(order.getUserId(), user), items, equipmentMap, warehouseMap);
    }

    private List<BorrowOrderVO> buildVos(List<BorrowOrder> orders) {
        if (orders.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = orders.stream()
                .map(BorrowOrder::getUserId)
                .distinct()
                .toList();
        Map<Long, SysUser> userMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
        List<Long> orderIds = orders.stream().map(BorrowOrder::getId).toList();
        Map<Long, List<BorrowItem>> itemsByOrder = itemMapper.selectList(
                        Wrappers.<BorrowItem>lambdaQuery().in(BorrowItem::getBorrowId, orderIds))
                .stream()
                .collect(Collectors.groupingBy(BorrowItem::getBorrowId));
        List<Long> equipmentIds = itemsByOrder.values().stream()
                .flatMap(List::stream)
                .map(BorrowItem::getEquipmentId)
                .distinct()
                .toList();
        Map<Long, Equipment> equipmentMap = equipmentIds.isEmpty() ? Map.of()
                : equipmentMapper.selectBatchIds(equipmentIds).stream()
                        .collect(Collectors.toMap(Equipment::getId, e -> e, (a, b) -> a));
        List<Long> warehouseIds = itemsByOrder.values().stream()
                .flatMap(List::stream)
                .map(BorrowItem::getWarehouseId)
                .distinct()
                .toList();
        Map<Long, Warehouse> warehouseMap = warehouseIds.isEmpty() ? Map.of()
                : warehouseMapper.selectBatchIds(warehouseIds).stream()
                        .collect(Collectors.toMap(Warehouse::getId, w -> w, (a, b) -> a));
        return orders.stream()
                .map(order -> toVo(order, userMap,
                        itemsByOrder.getOrDefault(order.getId(), List.of()),
                        equipmentMap, warehouseMap))
                .toList();
    }

    private BorrowOrderVO toVo(BorrowOrder order, Map<Long, SysUser> userMap,
                               List<BorrowItem> items,
                               Map<Long, Equipment> equipmentMap,
                               Map<Long, Warehouse> warehouseMap) {
        SysUser user = userMap.get(order.getUserId());
        if (user == null) {
            user = new SysUser();
        }
        List<BorrowItemVO> itemVos = items.stream().map(item -> {
            Equipment equipment = equipmentMap.get(item.getEquipmentId());
            Warehouse warehouse = warehouseMap.get(item.getWarehouseId());
            return new BorrowItemVO(item.getId(), item.getEquipmentId(),
                    equipment == null ? null : equipment.getEquipmentCode(),
                    equipment == null ? null : equipment.getEquipmentName(),
                    equipment == null ? null : equipment.getUnit(),
                    item.getWarehouseId(),
                    warehouse == null ? null : warehouse.getWarehouseName(),
                    item.getQuantity(), item.getIssuedQuantity(), item.getReturnedQuantity(),
                    item.getExpectedReturnDate(), item.getOverdueFlag(), item.getOverdueDays(), item.getStatus());
        }).toList();
        return new BorrowOrderVO(order.getId(), order.getOrderNo(), order.getUserId(),
                user.getUsername(), user.getRealName(), order.getBorrowType(), order.getPurpose(),
                order.getExpectedReturnDate(), order.getTotalQuantity(), order.getStatus(),
                order.getExtendCount(), order.getAuditRemark(), order.getRemark(),
                order.getCreateTime(), itemVos);
    }
}
