package com.company.sportseq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.sportseq.entity.EquipmentStock;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface EquipmentStockMapper extends BaseMapper<EquipmentStock> {

    /**
     * 库存增加（入库/归还/盘盈）：无条件原子累加。
     */
    @Update("UPDATE equipment_stock SET quantity = quantity + #{quantity}, update_by = #{updateBy}, " +
            "update_time = NOW(), version = version + 1 " +
            "WHERE equipment_id = #{equipmentId} AND warehouse_id = #{warehouseId}")
    int addQuantity(@Param("equipmentId") Long equipmentId,
                    @Param("warehouseId") Long warehouseId,
                    @Param("quantity") int quantity,
                    @Param("updateBy") Long updateBy);

    /**
     * 库存扣减（借用/报废/盘亏）：带数量条件，防止扣成负数。
     */
    @Update("UPDATE equipment_stock SET quantity = quantity - #{quantity}, update_by = #{updateBy}, " +
            "update_time = NOW(), version = version + 1 " +
            "WHERE equipment_id = #{equipmentId} AND warehouse_id = #{warehouseId} AND quantity >= #{quantity}")
    int subtractQuantity(@Param("equipmentId") Long equipmentId,
                         @Param("warehouseId") Long warehouseId,
                         @Param("quantity") int quantity,
                         @Param("updateBy") Long updateBy);

    /**
     * 按可用库存扣减（报废处置/盘亏）：不占用已锁定数量，防止扣掉待发放的锁定库存。
     */
    @Update("UPDATE equipment_stock SET quantity = quantity - #{quantity}, update_by = #{updateBy}, " +
            "update_time = NOW(), version = version + 1 " +
            "WHERE equipment_id = #{equipmentId} AND warehouse_id = #{warehouseId} " +
            "AND (quantity - locked_quantity) >= #{quantity}")
    int subtractAvailableQuantity(@Param("equipmentId") Long equipmentId,
                                  @Param("warehouseId") Long warehouseId,
                                  @Param("quantity") int quantity,
                                  @Param("updateBy") Long updateBy);

    /**
     * 锁定库存（借用申请提交）：条件 quantity - locked >= qty，防止并发超借。
     */
    @Update("UPDATE equipment_stock SET locked_quantity = locked_quantity + #{quantity}, " +
            "update_by = #{updateBy}, update_time = NOW(), version = version + 1 " +
            "WHERE equipment_id = #{equipmentId} AND warehouse_id = #{warehouseId} " +
            "AND (quantity - locked_quantity) >= #{quantity}")
    int lockQuantity(@Param("equipmentId") Long equipmentId,
                     @Param("warehouseId") Long warehouseId,
                     @Param("quantity") int quantity,
                     @Param("updateBy") Long updateBy);

    /**
     * 解锁库存（驳回/取消）：locked_quantity 扣减。
     */
    @Update("UPDATE equipment_stock SET locked_quantity = locked_quantity - #{quantity}, " +
            "update_by = #{updateBy}, update_time = NOW(), version = version + 1 " +
            "WHERE equipment_id = #{equipmentId} AND warehouse_id = #{warehouseId} " +
            "AND locked_quantity >= #{quantity}")
    int unlockQuantity(@Param("equipmentId") Long equipmentId,
                       @Param("warehouseId") Long warehouseId,
                       @Param("quantity") int quantity,
                       @Param("updateBy") Long updateBy);

    @Select("SELECT * FROM equipment_stock WHERE equipment_id = #{equipmentId} " +
            "AND warehouse_id = #{warehouseId} FOR UPDATE")
    EquipmentStock selectForUpdate(@Param("equipmentId") Long equipmentId,
                                   @Param("warehouseId") Long warehouseId);
}
