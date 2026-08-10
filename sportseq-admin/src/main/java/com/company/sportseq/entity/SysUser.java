package com.company.sportseq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.sportseq.common.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long deptId;
    private String username;
    private String password;
    private String nickname;
    private String realName;
    private String phone;
    private String email;
    private Integer gender;
    private String userType;
    private String avatar;
    private Integer status;
    private String loginIp;
    private LocalDateTime loginDate;
    private LocalDateTime pwdUpdateDate;
    private Integer loginFailCount;
    private LocalDateTime lockTime;
    private String remark;
}
