package com.erp.system.aspect;

import com.erp.common.annotation.OperLog;
import com.erp.common.security.LoginUser;
import com.erp.common.security.SecurityUtils;
import com.erp.system.entity.SysOperLog;
import com.erp.system.service.LogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 操作日志切面：记录模块、动作、参数（脱敏）、结果与耗时。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {

    private final LogService logService;
    private final ObjectMapper objectMapper;

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();
        SysOperLog record = new SysOperLog();
        record.setModule(operLog.module());
        record.setAction(operLog.action());
        record.setMethod(((MethodSignature) joinPoint.getSignature()).getMethod().getName());
        record.setParams(maskParams(joinPoint.getArgs()));
        record.setCreatedAt(LocalDateTime.now());
        record.setIp(currentIp());
        LoginUser user = SecurityUtils.getLoginUserOrNull();
        if (user != null) {
            record.setOperatorId(user.getUserId());
            record.setOperatorName(user.getRealName());
        }
        try {
            Object result = joinPoint.proceed();
            record.setSuccess(1);
            record.setResult("OK");
            return result;
        } catch (Throwable ex) {
            record.setSuccess(0);
            String message = ex.getMessage();
            record.setResult(message != null && message.length() > 500 ? message.substring(0, 500) : message);
            throw ex;
        } finally {
            record.setCostMs(System.currentTimeMillis() - start);
            try {
                logService.recordOper(record);
            } catch (Exception e) {
                log.warn("操作日志记录失败", e);
            }
        }
    }

    private String maskParams(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(Arrays.stream(args)
                    .filter(a -> a != null && !(a instanceof HttpServletRequest))
                    .toList());
            String masked = json.replaceAll("(\"password\"\\s*:\\s*\")([^\"]*)(\")", "$1******$3");
            return masked.length() > 2000 ? masked.substring(0, 2000) : masked;
        } catch (Exception e) {
            return null;
        }
    }

    private String currentIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null ? forwarded.split(",")[0] : request.getRemoteAddr();
    }
}
