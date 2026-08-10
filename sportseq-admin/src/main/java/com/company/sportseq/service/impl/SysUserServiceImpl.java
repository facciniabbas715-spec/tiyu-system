package com.company.sportseq.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.sportseq.common.constant.CacheConstants;
import com.company.sportseq.common.exception.BizException;
import com.company.sportseq.common.exception.ErrorCode;
import com.company.sportseq.common.result.PageResult;
import com.company.sportseq.dto.ResetPasswordDTO;
import com.company.sportseq.dto.UserDTO;
import com.company.sportseq.dto.UserStatusDTO;
import com.company.sportseq.entity.SysDept;
import com.company.sportseq.entity.SysUser;
import com.company.sportseq.entity.SysUserRole;
import com.company.sportseq.mapper.SysDeptMapper;
import com.company.sportseq.mapper.SysUserMapper;
import com.company.sportseq.mapper.SysUserRoleMapper;
import com.company.sportseq.security.SecurityUtils;
import com.company.sportseq.service.SysUserService;
import com.company.sportseq.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    @Override
    public PageResult<UserVO> page(long current, long size, String username, String phone,
                                   Integer status, Long deptId) {
        Page<SysUser> page = userMapper.selectPage(new Page<>(current, size),
                Wrappers.<SysUser>lambdaQuery()
                        .like(StrUtil.isNotBlank(username), SysUser::getUsername, username)
                        .like(StrUtil.isNotBlank(phone), SysUser::getPhone, phone)
                        .eq(status != null, SysUser::getStatus, status)
                        .eq(deptId != null, SysUser::getDeptId, deptId)
                        .orderByDesc(SysUser::getCreateTime));
        List<UserVO> records = page.getRecords().stream().map(this::toVo).toList();
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    @Override
    public UserVO detail(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "用户不存在");
        }
        return toVo(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(UserDTO dto) {
        checkUsernameUnique(dto.getUsername(), null);
        if (StrUtil.isBlank(dto.getPassword())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "新增用户必须设置密码");
        }
        SysUser user = new SysUser();
        applyUserFields(user, dto);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setLoginFailCount(0);
        userMapper.insert(user);
        assignRoles(user.getId(), dto.getRoleIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(UserDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ErrorCode.PARAM_ERROR, "用户ID不能为空");
        }
        checkUsernameUnique(dto.getUsername(), dto.getId());
        SysUser user = new SysUser();
        user.setId(dto.getId());
        applyUserFields(user, dto);
        userMapper.updateById(user);
        userRoleMapper.deleteByUserId(dto.getId());
        assignRoles(dto.getId(), dto.getRoleIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        if (id.equals(SecurityUtils.getUserId())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "不能删除当前登录用户");
        }
        userMapper.deleteById(id);
        userRoleMapper.deleteByUserId(id);
    }

    @Override
    public void resetPassword(ResetPasswordDTO dto) {
        SysUser update = new SysUser();
        update.setId(dto.getUserId());
        update.setPassword(passwordEncoder.encode(dto.getPassword()));
        update.setLoginFailCount(0);
        update.setLockTime(null);
        userMapper.updateById(update);
    }

    @Override
    public void changeStatus(UserStatusDTO dto) {
        if (dto.getStatus() == 0 && dto.getUserId().equals(SecurityUtils.getUserId())) {
            throw new BizException(ErrorCode.PARAM_ERROR, "不能停用当前登录用户");
        }
        SysUser update = new SysUser();
        update.setId(dto.getUserId());
        update.setStatus(dto.getStatus());
        userMapper.updateById(update);
        if (dto.getStatus() == 0) {
            kickUserSessions(dto.getUserId());
        }
    }

    private void kickUserSessions(Long userId) {
        Set<String> jtis = redisTemplate.opsForSet().members(CacheConstants.LOGIN_USER_KEY + userId);
        if (jtis != null) {
            for (String jti : jtis) {
                redisTemplate.delete(CacheConstants.LOGIN_TOKEN_KEY + jti);
            }
        }
        redisTemplate.delete(CacheConstants.LOGIN_USER_KEY + userId);
    }

    private void checkUsernameUnique(String username, Long excludeId) {
        Long count = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username)
                .ne(excludeId != null, SysUser::getId, excludeId));
        if (count > 0) {
            throw new BizException(ErrorCode.PARAM_ERROR, "用户名已存在");
        }
    }

    private void applyUserFields(SysUser user, UserDTO dto) {
        user.setDeptId(dto.getDeptId());
        user.setUsername(dto.getUsername());
        user.setNickname(dto.getNickname());
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setGender(dto.getGender() == null ? 0 : dto.getGender());
        user.setUserType(dto.getUserType() == null ? "other" : dto.getUserType());
        user.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        user.setRemark(dto.getRemark());
    }

    private void assignRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null) {
            return;
        }
        for (Long roleId : roleIds) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            userRoleMapper.insert(relation);
        }
    }

    private UserVO toVo(SysUser user) {
        String deptName = null;
        if (user.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(user.getDeptId());
            deptName = dept == null ? null : dept.getDeptName();
        }
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(user.getId());
        return new UserVO(user.getId(), user.getDeptId(), deptName, user.getUsername(),
                user.getNickname(), user.getRealName(), user.getPhone(), user.getEmail(),
                user.getGender(), user.getUserType(), user.getStatus(), user.getLoginDate(), roleIds);
    }
}
