package com.company.sportseq.security;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.company.sportseq.common.constant.CacheConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器：解析 Bearer Token -> Redis 会话校验 -> 填充 SecurityContext。
 * 会话不存在（登出/踢出/过期）时仅不认证，由后续安全链返回 401。
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (StrUtil.isNotBlank(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String jti = jwtUtil.parseToken(token).getId();
                String sessionJson = redisTemplate.opsForValue().get(CacheConstants.LOGIN_TOKEN_KEY + jti);
                if (StrUtil.isNotBlank(sessionJson)) {
                    LoginUser loginUser = JSONUtil.toBean(sessionJson, LoginUser.class);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                log.debug("JWT 解析失败: {}", e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StrUtil.isNotBlank(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
