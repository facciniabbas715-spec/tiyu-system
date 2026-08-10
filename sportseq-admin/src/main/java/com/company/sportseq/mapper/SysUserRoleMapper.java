package com.company.sportseq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.sportseq.entity.SysUserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(Long userId);

    @Select("SELECT role_id FROM sys_user_role WHERE user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(Long userId);
}
