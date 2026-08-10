package com.company.sportseq.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.company.sportseq.annotation.Log;
import com.company.sportseq.common.utils.IpUtils;
import com.company.sportseq.entity.SysOperLog;
import com.company.sportseq.mapper.SysOperLogMapper;
import com.company.sportseq.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 操作日志切面：@Log 方法执行前后采集信息，异步写入 sys_oper_log。
 * 敏感参数（密码/令牌）序列化后统一脱敏。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private static final int MAX_TEXT_LENGTH = 2000;

    private final SysOperLogMapper operLogMapper;

    @Around("@annotation(controllerLog)")
    public Object around(ProceedingJoinPoint point, Log controllerLog) throws Throwable {
        long start = System.currentTimeMillis();
        SysOperLog operLog = new SysOperLog();
        try {
            Object result = point.proceed();
            operLog.setStatus(0);
            operLog.setJsonResult(StrUtil.sub(maskSensitive(JSONUtil.toJsonStr(result)), 0, MAX_TEXT_LENGTH));
            return result;
        } catch (Throwable e) {
            operLog.setStatus(1);
            operLog.setErrorMsg(StrUtil.sub(e.getMessage(), 0, MAX_TEXT_LENGTH));
            throw e;
        } finally {
            fillBase(operLog, point, controllerLog, System.currentTimeMillis() - start);
            asyncInsert(operLog);
        }
    }

    @Async("asyncExecutor")
    public void asyncInsert(SysOperLog operLog) {
        try {
            operLogMapper.insert(operLog);
        } catch (Exception e) {
            log.error("操作日志写入失败: {}", e.getMessage());
        }
    }

    private void fillBase(SysOperLog operLog, ProceedingJoinPoint point, Log controllerLog, long cost) {
        operLog.setTitle(controllerLog.title());
        operLog.setBusinessType(controllerLog.businessType());
        operLog.setMethod(point.getSignature().getDeclaringTypeName() + "." + point.getSignature().getName());
        operLog.setCostTime(cost);
        operLog.setOperTime(LocalDateTime.now());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            operLog.setOperName(loginUser.getUsername());
        }
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            operLog.setRequestMethod(request.getMethod());
            operLog.setOperUrl(StrUtil.sub(request.getRequestURI(), 0, 255));
            operLog.setOperIp(IpUtils.getClientIp(request));
            Object[] args = point.getArgs();
            if (args != null && args.length > 0) {
                operLog.setOperParam(StrUtil.sub(maskSensitive(JSONUtil.toJsonStr(args)), 0, MAX_TEXT_LENGTH));
            }
        }
    }

    private String maskSensitive(String json) {
        if (StrUtil.isBlank(json)) {
            return json;
        }
        return json.replaceAll(
                "(\"(password|oldPassword|newPassword|token|Authorization)\"\\s*:\\s*)\"[^\"]*\"",
                "$1\"***\"");
    }
}
