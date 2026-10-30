package com.erp.system.controller;

import com.erp.common.annotation.OperLog;
import com.erp.common.api.PageResult;
import com.erp.common.api.Result;
import com.erp.system.dto.UserSaveRequest;
import com.erp.system.entity.SysUser;
import com.erp.system.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<PageResult<SysUser>> page(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Long deptId,
                                            @RequestParam(required = false) Integer status) {
        return Result.ok(userService.page(page, size, keyword, deptId, status));
    }

    @GetMapping("/options")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<List<Map<String, Object>>> options() {
        List<Map<String, Object>> options = userService.listAll().stream()
                .map(u -> Map.<String, Object>of("id", u.getId(), "name", u.getRealName()))
                .toList();
        return Result.ok(options);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:view')")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        user.setPassword(null);
        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        data.put("roleIds", userService.listRoleIds(id));
        return Result.ok(data);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:user:create')")
    @OperLog(module = "系统管理", action = "新增用户")
    public Result<Long> create(@Valid @RequestBody UserSaveRequest request) {
        return Result.ok(userService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:update')")
    @OperLog(module = "系统管理", action = "修改用户")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UserSaveRequest request) {
        userService.update(id, request);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('system:user:update')")
    @OperLog(module = "系统管理", action = "修改用户状态")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return Result.ok();
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasAuthority('system:user:reset')")
    @OperLog(module = "系统管理", action = "重置密码")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        userService.resetPassword(id, body.get("password"));
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    @OperLog(module = "系统管理", action = "删除用户")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.ok();
    }
}
