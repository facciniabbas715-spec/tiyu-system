package com.company.sportseq.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.jsonwebtoken.Claims;
import com.company.sportseq.common.constant.CacheConstants;
import com.company.sportseq.common.exception.BizException;
import com.company.sportseq.common.exception.ErrorCode;
import com.company.sportseq.common.utils.IpUtils;
import com.company.sportseq.dto.LoginDTO;
import com.company.sportseq.dto.UpdatePasswordDTO;
import com.company.sportseq.entity.SysLoginLog;
import com.company.sportseq.entity.SysUser;
import com.company.sportseq.mapper.SysLoginLogMapper;
import com.company.sportseq.mapper.SysMenuMapper;
import com.company.sportseq.mapper.SysRoleMapper;
import com.company.sportseq.mapper.SysUserMapper;
import com.company.sportseq.security.JwtUtil;
import com.company.sportseq.security.LoginUser;
import com.company.sportseq.security.SecurityUtils;
import com.company.sportseq.service.AuthService;
import com.company.sportseq.vo.CaptchaVO;
import com.company.sportseq.vo.LoginVO;
import com.company.sportseq.vo.UserInfoVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int MAX_LOGIN_FAIL_COUNT = 5;
    private static final long LOCK_MINUTES = 10;

    private final SysUserMapper userMapper;
    private final SysMenuMapper menuMapper;
    private final SysRoleMapper roleMapper;
    private final SysLoginLogMapper loginLogMapper;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${jwt.expiration}")
    private long tokenExpirationSeconds;

    @Override
    public CaptchaVO captcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 20);
        String uuid = IdUtil.fastSimpleUUID();
        redisTemplate.opsForValue().set(
                CacheConstants.CAPTCHA_CODE_KEY + uuid,
                captcha.getCode(),
                Duration.ofMinutes(CacheConstants.CAPTCHA_EXPIRATION_MINUTES));
        return new CaptchaVO(uuid, captcha.getImageBase64Data());
    }

    @Override
    public LoginVO login(LoginDTO dto, HttpServletRequest request) {
        validateCaptcha(dto.getUuid(), dto.getCode());

        SysUser user = userMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, dto.getUsername()));
        if (user == null) {
            saveLoginLog(dto.getUsername(), request, false, "账号或密码错误");
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        checkLocked(user);
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            handleLoginFail(user, request);
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            saveLoginLog(dto.getUsername(), request, false, "账号已停用");
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }

        handleLoginSuccess(user, request);
        Set<String> permissions = menuMapper.selectPermsByUserId(user.getId());
        Set<String> roles = roleMapper.selectRoleKeysByUserId(user.getId());
        LoginUser loginUser = new LoginUser(user.getId(), user.getDeptId(), user.getUsername(),
                user.getPassword(), user.getRealName(), permissions, roles);

        String jti = IdUtil.fastSimpleUUID();
        String token = jwtUtil.createToken(user.getId(), user.getUsername(), jti);
        redisTemplate.opsForValue().set(
                CacheConstants.LOGIN_TOKEN_KEY + jti,
                JSONUtil.toJsonStr(loginUser),
                Duration.ofSeconds(tokenExpirationSeconds));
        redisTemplate.opsForSet().add(CacheConstants.LOGIN_USER_KEY + user.getId(), jti);

        saveLoginLog(dto.getUsername(), request, true, "登录成功");
        return new LoginVO(token, toUserInfo(loginUser));
    }

    @Override
    public void logout(HttpServletRequest request) {
        String token = resolveToken(request);
        if (StrUtil.isNotBlank(token)) {
            try {
                Claims claims = jwtUtil.parseToken(token);
                String jti = claims.getId();
                Long userId = claims.get("userId", Long.class);
                redisTemplate.delete(CacheConstants.LOGIN_TOKEN_KEY + jti);
                if (userId != null) {
                    redisTemplate.opsForSet().remove(CacheConstants.LOGIN_USER_KEY + userId, jti);
                }
            } catch (Exception ignored) {
                // token 已失效则无需清理
            }
        }
    }

    @Override
    public UserInfoVO info() {
        return toUserInfo(SecurityUtils.getLoginUser());
    }

    @Override
    public void updatePassword(UpdatePasswordDTO dto, HttpServletRequest request) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser user = userMapper.selectById(loginUser.getUserId());
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "旧密码错误");
        }
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        update.setPwdUpdateDate(LocalDateTime.now());
        userMapper.updateById(update);
        // 改密后使当前会话失效，强制重新登录
        String token = resolveToken(request);
        if (StrUtil.isNotBlank(token)) {
            try {
                redisTemplate.delete(CacheConstants.LOGIN_TOKEN_KEY + jwtUtil.parseToken(token).getId());
            } catch (Exception ignored) {
            }
        }
    }

    private void validateCaptcha(String uuid, String code) {
        String key = CacheConstants.CAPTCHA_CODE_KEY + uuid;
        String cached = redisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(cached)) {
            throw new BizException(ErrorCode.CAPTCHA_ERROR, "验证码已过期，请刷新重试");
        }
        if (!cached.equalsIgnoreCase(code)) {
            throw new BizException(ErrorCode.CAPTCHA_ERROR);
        }
        redisTemplate.delete(key);
    }

    private void checkLocked(SysUser user) {
        if (user.getLoginFailCount() != null && user.getLoginFailCount() >= MAX_LOGIN_FAIL_COUNT
                && user.getLockTime() != null && user.getLockTime().isAfter(LocalDateTime.now())) {
            throw new BizException(ErrorCode.ACCOUNT_LOCKED);
        }
    }

    private void handleLoginFail(SysUser user, HttpServletRequest request) {
        int failCount = (user.getLoginFailCount() == null ? 0 : user.getLoginFailCount()) + 1;
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setLoginFailCount(failCount);
        if (failCount >= MAX_LOGIN_FAIL_COUNT) {
            update.setLockTime(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
        }
        userMapper.updateById(update);
        saveLoginLog(user.getUsername(), request, false,
                failCount >= MAX_LOGIN_FAIL_COUNT ? "密码错误，账号已锁定10分钟" : "密码错误");
    }

    private void handleLoginSuccess(SysUser user, HttpServletRequest request) {
        SysUser update = new SysUser();
        update.setId(user.getId());
        update.setLoginFailCount(0);
        update.setLockTime(null);
        update.setLoginIp(IpUtils.getClientIp(request));
        update.setLoginDate(LocalDateTime.now());
        userMapper.updateById(update);
    }

    private void saveLoginLog(String username, HttpServletRequest request, boolean success, String msg) {
        SysLoginLog log = new SysLoginLog();
        log.setUserName(username);
        log.setIpaddr(IpUtils.getClientIp(request));
        UserAgent ua = UserAgentUtil.parse(request.getHeader("User-Agent"));
        if (ua != null) {
            log.setBrowser(ua.getBrowser().getName());
            log.setOs(ua.getOs().getName());
        }
        log.setStatus(success ? 1 : 0);
        log.setMsg(msg);
        log.setLoginTime(LocalDateTime.now());
        loginLogMapper.insert(log);
    }

    private UserInfoVO toUserInfo(LoginUser loginUser) {
        return new UserInfoVO(
                loginUser.getUserId(),
                loginUser.getUsername(),
                loginUser.getRealName(),
                null,
                null,
                loginUser.getPermissions(),
                loginUser.getRoles());
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StrUtil.isNotBlank(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
