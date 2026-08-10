package com.company.sportseq.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.company.sportseq.common.exception.BizException;
import com.company.sportseq.common.exception.ErrorCode;
import com.company.sportseq.entity.SysUser;
import com.company.sportseq.mapper.SysMenuMapper;
import com.company.sportseq.mapper.SysRoleMapper;
import com.company.sportseq.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMapper roleMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        Set<String> permissions = menuMapper.selectPermsByUserId(user.getId());
        Set<String> roles = roleMapper.selectRoleKeysByUserId(user.getId());
        return new LoginUser(user.getId(), user.getDeptId(), user.getUsername(),
                user.getPassword(), user.getRealName(), permissions, roles);
    }
}
