package com.company.sportseq.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.company.sportseq.entity.BorrowItem;
import com.company.sportseq.mapper.BorrowItemMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 逾期扫描：每日凌晨标记超期未还的借用明细。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BorrowOverdueJob {

    private final BorrowItemMapper borrowItemMapper;

    @Scheduled(cron = "0 0 2 * * ?")
    public void scanOverdue() {
        List<BorrowItem> overdueItems = borrowItemMapper.selectList(
                Wrappers.<BorrowItem>lambdaQuery()
                        .in(BorrowItem::getStatus, 1, 2)
                        .lt(BorrowItem::getExpectedReturnDate, LocalDate.now()));
        for (BorrowItem item : overdueItems) {
            BorrowItem update = new BorrowItem();
            update.setId(item.getId());
            update.setOverdueFlag(1);
            update.setOverdueDays((int) ChronoUnit.DAYS.between(item.getExpectedReturnDate(), LocalDate.now()));
            borrowItemMapper.updateById(update);
        }
        if (!overdueItems.isEmpty()) {
            log.info("逾期扫描完成，标记 {} 条借用明细", overdueItems.size());
        }
    }
}
