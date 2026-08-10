package com.company.sportseq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.sportseq.entity.SysMenu;
import org.apache.ibatis.annotations.Select;

import java.util.Set;

public interface SysMenuMapper extends BaseMapper<SysMenu> {

    @Select("SELECT DISTINCT m.perms FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
            "INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND m.perms IS NOT NULL AND m.perms <> '' AND m.status = 1")
    Set<String> selectPermsByUserId(Long userId);
}
