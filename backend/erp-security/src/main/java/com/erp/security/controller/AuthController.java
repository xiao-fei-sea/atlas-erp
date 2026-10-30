package com.erp.security.controller;

import com.erp.common.annotation.OperLog;
import com.erp.common.api.ErrorCode;
import com.erp.common.api.Result;
import com.erp.common.exception.BizException;
import com.erp.common.security.LoginUser;
import com.erp.common.security.SecurityUtils;
import com.erp.security.dto.LoginRequest;
import com.erp.security.jwt.JwtTokenProvider;
import com.erp.security.jwt.JwtProperties;
import com.erp.system.entity.SysUser;
import com.erp.system.service.LogService;
import com.erp.system.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;
    private final UserService userService;
    private final LogService logService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        String ip = clientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (AuthenticationException ex) {
            logService.recordLogin(request.getUsername(), ip, userAgent, false, ex.getMessage());
            throw new BizException(ErrorCode.BAD_CREDENTIALS);
        }
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        logService.recordLogin(loginUser.getUsername(), ip, userAgent, true, "登录成功");
        userService.touchLogin(loginUser.getUserId());

        Map<String, Object> data = new HashMap<>();
        data.put("token", tokenProvider.generate(loginUser));
        data.put("tokenType", "Bearer");
        data.put("expiresIn", jwtProperties.getExpireMinutes() * 60);
        data.put("user", userInfo(loginUser));
        return Result.ok(data);
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        return Result.ok(userInfo(SecurityUtils.getLoginUser()));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        logService.recordLogin(loginUser.getUsername(), null, null, true, "退出登录");
        return Result.ok();
    }

    @PostMapping("/password")
    @OperLog(module = "认证", action = "修改密码")
    public Result<Void> changePassword(@RequestBody Map<String, String> body) {
        userService.changePassword(SecurityUtils.getUserId(), body.get("oldPassword"), body.get("newPassword"));
        return Result.ok();
    }

    private Map<String, Object> userInfo(LoginUser loginUser) {
        SysUser user = userService.getById(loginUser.getUserId());
        Map<String, Object> info = new HashMap<>();
        info.put("id", loginUser.getUserId());
        info.put("username", loginUser.getUsername());
        info.put("realName", loginUser.getRealName());
        info.put("deptId", loginUser.getDeptId());
        info.put("dataScope", loginUser.getDataScope());
        info.put("roles", loginUser.getRoleCodes());
        info.put("permissions", loginUser.getPermissions());
        return info;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null ? forwarded.split(",")[0] : request.getRemoteAddr();
    }
}
