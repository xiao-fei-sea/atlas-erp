package com.erp.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.api.ErrorCode;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.common.security.LoginUser;
import com.erp.common.security.SecurityUtils;
import com.erp.system.dto.UserSaveRequest;
import com.erp.system.entity.SysRole;
import com.erp.system.entity.SysUser;
import com.erp.system.entity.SysUserRole;
import com.erp.system.mapper.SysMenuMapper;
import com.erp.system.mapper.SysRoleMapper;
import com.erp.system.mapper.SysUserMapper;
import com.erp.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户与登录上下文服务。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final PasswordEncoder passwordEncoder;

    public SysUser getByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username));
    }

    public SysUser getById(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BizException.notFound("用户不存在");
        }
        return user;
    }

    public PageResult<SysUser> page(long page, long size, String keyword, Long deptId, Integer status) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }
        wrapper.eq(deptId != null, SysUser::getDeptId, deptId)
                .eq(status != null, SysUser::getStatus, status)
                .orderByDesc(SysUser::getId);
        Page<SysUser> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        result.getRecords().forEach(u -> u.setPassword(null));
        return PageResult.of(result);
    }

    public List<Long> listRoleIds(Long userId) {
        return userRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
    }

    public List<Long> userIdsByRoleCode(String roleCode) {
        return userRoleMapper.selectUserIdsByRoleCode(roleCode);
    }

    public LoginUser buildLoginUser(SysUser user) {
        List<SysRole> roles = roleMapper.selectByUserId(user.getId());
        Set<String> permissions = new HashSet<>(menuMapper.selectPermissionsByUserId(user.getId()));
        Set<String> roleCodes = new HashSet<>();
        String dataScope = "SELF";
        for (SysRole role : roles) {
            roleCodes.add(role.getCode());
            permissions.add("ROLE_" + role.getCode());
            dataScope = widerScope(dataScope, role.getDataScope());
        }
        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setPassword(user.getPassword());
        loginUser.setRealName(user.getRealName());
        loginUser.setDeptId(user.getDeptId());
        loginUser.setDataScope(dataScope);
        loginUser.setEnabled(user.getStatus() != null && user.getStatus() == 1);
        loginUser.setPermissions(permissions);
        loginUser.setRoleCodes(roleCodes);
        return loginUser;
    }

    private String widerScope(String current, String candidate) {
        List<String> order = List.of("ALL", "DEPT_AND_CHILD", "DEPT", "SELF");
        int currentIdx = order.indexOf(current) < 0 ? order.size() - 1 : order.indexOf(current);
        int candidateIdx = order.indexOf(candidate) < 0 ? order.size() - 1 : order.indexOf(candidate);
        return order.get(Math.min(currentIdx, candidateIdx));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(UserSaveRequest request) {
        if (getByUsername(request.getUsername()) != null) {
            throw new BizException(ErrorCode.DUPLICATE, "用户名已存在");
        }
        String rawPassword = StringUtils.hasText(request.getPassword()) ? request.getPassword() : "Erp@123456";
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRealName(request.getRealName());
        user.setDeptId(request.getDeptId());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        user.setRemark(request.getRemark());
        userMapper.insert(user);
        saveRoles(user.getId(), request.getRoleIds());
        return user.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, UserSaveRequest request) {
        SysUser user = getById(id);
        SysUser exists = getByUsername(request.getUsername());
        if (exists != null && !exists.getId().equals(id)) {
            throw new BizException(ErrorCode.DUPLICATE, "用户名已存在");
        }
        user.setUsername(request.getUsername());
        user.setRealName(request.getRealName());
        user.setDeptId(request.getDeptId());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setStatus(request.getStatus());
        user.setRemark(request.getRemark());
        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        userMapper.updateById(user);
        if (request.getRoleIds() != null) {
            userRoleMapper.deleteByUserId(id);
            saveRoles(id, request.getRoleIds());
        }
    }

    private void saveRoles(Long userId, List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        for (Long roleId : roleIds) {
            SysUserRole relation = new SysUserRole();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            userRoleMapper.insert(relation);
        }
    }

    public void touchLogin(Long id) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setLastLoginAt(java.time.LocalDateTime.now());
        userMapper.updateById(user);
    }

    public void updateStatus(Long id, Integer status) {
        SysUser user = getById(id);
        user.setStatus(status);
        userMapper.updateById(user);
    }

    public void resetPassword(Long id, String rawPassword) {
        SysUser user = getById(id);
        user.setPassword(passwordEncoder.encode(StringUtils.hasText(rawPassword) ? rawPassword : "Erp@123456"));
        userMapper.updateById(user);
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = getById(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "原密码不正确");
        }
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6) {
            throw new BizException(ErrorCode.BAD_REQUEST, "新密码长度不能少于6位");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    public void delete(Long id) {
        SysUser user = getById(id);
        if ("admin".equals(user.getUsername())) {
            throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "内置管理员不可删除");
        }
        if (id.equals(SecurityUtils.getUserId())) {
            throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "不能删除当前登录用户");
        }
        userMapper.deleteById(id);
        userRoleMapper.deleteByUserId(id);
    }

    public List<SysUser> listAll() {
        return userMapper.selectList(new LambdaQueryWrapper<SysUser>().orderByAsc(SysUser::getId));
    }

    public List<SysUser> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        return userMapper.selectBatchIds(ids);
    }
}
