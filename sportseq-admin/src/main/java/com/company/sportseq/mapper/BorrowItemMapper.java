package com.company.sportseq.mapper;

import com.company.sportseq.entity.BorrowItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface BorrowItemMapper extends BaseMapper<BorrowItem> {

    /**
     * 行锁读取借用明细：归还确认时防止并发重复入账。
     */
    @Select("SELECT * FROM borrow_item WHERE id = #{id} FOR UPDATE")
    BorrowItem selectForUpdate(@Param("id") Long id);
}
