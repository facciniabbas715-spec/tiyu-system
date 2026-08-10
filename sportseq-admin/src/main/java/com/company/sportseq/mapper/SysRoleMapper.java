package com.company.sportseq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.sportseq.entity.SysRole;
import org.apache.ibatis.annotations.Select;

import java.util.Set;

public interface SysRoleMapper extends BaseMapper<SysRole> {

    @Select("SELECT DISTINCT r.role_key FROM sys_role r " +
            "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.status = 1 AND r.del_flag = 0")
    Set<String> selectRoleKeysByUserId(Long userId);
}
