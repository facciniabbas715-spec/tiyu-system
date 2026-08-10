package com.company.sportseq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.sportseq.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String menuName;
    private Long parentId;
    private Integer orderNum;
    private String menuType;
    private String path;
    private String component;
    private String query;
    private String perms;
    private String icon;
    private Integer visible;
    private Integer status;
}
