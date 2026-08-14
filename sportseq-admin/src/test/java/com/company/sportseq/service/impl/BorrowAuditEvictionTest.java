package com.company.sportseq.service.impl;

import com.company.sportseq.common.cache.CacheService;
import com.company.sportseq.common.constant.CacheConstants;
import com.company.sportseq.dto.BorrowAuditDTO;
import com.company.sportseq.entity.BorrowItem;
import com.company.sportseq.entity.BorrowOrder;
import com.company.sportseq.mapper.BorrowItemMapper;
import com.company.sportseq.mapper.BorrowOrderMapper;
import com.company.sportseq.mapper.EquipmentMapper;
import com.company.sportseq.mapper.EquipmentStockMapper;
import com.company.sportseq.mapper.StockRecordMapper;
import com.company.sportseq.mapper.SysUserMapper;
import com.company.sportseq.mapper.WarehouseMapper;
import com.company.sportseq.service.DataScopeService;
import com.company.sportseq.security.LoginUser;
import com.company.sportseq.service.SysConfigService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 借用审核驳回的缓存一致性单元测试。
 *
 * <p>驳回会解锁库存并改变仪表盘"今日借用数"，必须在数据库变更前先删一次缓存，
 * 解锁完成后再删一次，避免读者在写库与删缓存之间回填脏数据。</p>
 */
class BorrowAuditEvictionTest {

    private BorrowOrderMapper orderMapper;
    private BorrowItemMapper itemMapper;
    private EquipmentStockMapper stockMapper;
    private DataScopeService dataScopeService;
    private CacheService cacheService;
    private BorrowServiceImpl service;

    @BeforeEach
    void setUp() {
        orderMapper = mock(BorrowOrderMapper.class);
        itemMapper = mock(BorrowItemMapper.class);
        stockMapper = mock(EquipmentStockMapper.class);
        EquipmentMapper equipmentMapper = mock(EquipmentMapper.class);
        WarehouseMapper warehouseMapper = mock(WarehouseMapper.class);
        SysUserMapper userMapper = mock(SysUserMapper.class);
        StockRecordMapper recordMapper = mock(StockRecordMapper.class);
        SysConfigService configService = mock(SysConfigService.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        dataScopeService = mock(DataScopeService.class);
        cacheService = mock(CacheService.class);
        // 无事务上下文时，evictAfterCommit 应立即执行（与真实 CacheService 行为一致）
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(cacheService).evictAfterCommit(any(Runnable.class));

        service = new BorrowServiceImpl(orderMapper, itemMapper, stockMapper, equipmentMapper,
                warehouseMapper, userMapper, recordMapper, configService, redisTemplate,
                dataScopeService, cacheService);

        LoginUser user = new LoginUser();
        user.setUserId(1L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void auditReject_shouldEvictStockCachesBeforeStatusUpdateAndAgainAfterUnlock() {
        BorrowOrder order = new BorrowOrder();
        order.setId(1L);
        order.setStatus(0);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(dataScopeService.isFullScope()).thenReturn(true);

        BorrowItem item = new BorrowItem();
        item.setId(10L);
        item.setEquipmentId(2L);
        item.setWarehouseId(3L);
        item.setQuantity(5);
        when(itemMapper.selectList(any())).thenReturn(List.of(item));
        when(stockMapper.unlockQuantity(2L, 3L, 5, 1L)).thenReturn(1);

        BorrowAuditDTO dto = new BorrowAuditDTO();
        dto.setOrderId(1L);
        dto.setPass(false);
        dto.setRemark("库存不足");

        service.audit(dto);

        InOrder inOrder = inOrder(cacheService, orderMapper, stockMapper);
        inOrder.verify(cacheService).evictByPattern(CacheConstants.STOCK_PAGE_PATTERN);
        inOrder.verify(cacheService).evict(CacheConstants.DASHBOARD_SUMMARY_KEY);
        inOrder.verify(orderMapper).updateById(any(BorrowOrder.class));
        inOrder.verify(stockMapper).unlockQuantity(eq(2L), eq(3L), eq(5), eq(1L));
        inOrder.verify(cacheService).evictByPattern(CacheConstants.STOCK_PAGE_PATTERN);
        inOrder.verify(cacheService).evict(CacheConstants.DASHBOARD_SUMMARY_KEY);
    }

    @Test
    void auditApprove_shouldNotTouchCache() {
        BorrowOrder order = new BorrowOrder();
        order.setId(1L);
        order.setStatus(0);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(dataScopeService.isFullScope()).thenReturn(true);

        BorrowAuditDTO dto = new BorrowAuditDTO();
        dto.setOrderId(1L);
        dto.setPass(true);
        dto.setRemark("同意");

        service.audit(dto);

        verifyNoInteractions(cacheService);
    }
}
