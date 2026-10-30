package com.erp.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.api.ErrorCode;
import com.erp.common.api.PageResult;
import com.erp.common.exception.BizException;
import com.erp.system.dto.RoleSaveRequest;
import com.erp.system.entity.SysRole;
import com.erp.system.entity.SysRoleMenu;
import com.erp.system.entity.SysUserRole;
import com.erp.system.mapper.SysRoleMapper;
import com.erp.system.mapper.SysRoleMenuMapper;
import com.erp.system.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysUserRoleMapper userRoleMapper;

    public PageResult<SysRole> page(long page, long size, String keyword, Integer status) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysRole::getName, keyword).or().like(SysRole::getCode, keyword));
        }
        wrapper.eq(status != null, SysRole::getStatus, status).orderByAsc(SysRole::getId);
        return PageResult.of(roleMapper.selectPage(new Page<>(page, size), wrapper));
    }

    public List<SysRole> listAll() {
        return roleMapper.selectList(new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getId));
    }

    public SysRole getById(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw BizException.notFound("角色不存在");
        }
        return role;
    }

    public List<Long> listMenuIds(Long roleId) {
        return roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                        .eq(SysRoleMenu::getRoleId, roleId))
                .stream().map(SysRoleMenu::getMenuId).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(RoleSaveRequest request) {
        Long count = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getCode, request.getCode()));
        if (count > 0) {
            throw new BizException(ErrorCode.DUPLICATE, "角色编码已存在");
        }
        SysRole role = new SysRole();
        role.setCode(request.getCode());
        role.setName(request.getName());
        role.setDataScope(request.getDataScope());
        role.setRemark(request.getRemark());
        role.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        roleMapper.insert(role);
        saveMenus(role.getId(), request.getMenuIds());
        return role.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, RoleSaveRequest request) {
        SysRole role = getById(id);
        role.setCode(request.getCode());
        role.setName(request.getName());
        role.setDataScope(request.getDataScope());
        role.setRemark(request.getRemark());
        role.setStatus(request.getStatus());
        roleMapper.updateById(role);
        if (request.getMenuIds() != null) {
            roleMenuMapper.deleteByRoleId(id);
            saveMenus(id, request.getMenuIds());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(Long roleId, List<Long> menuIds) {
        getById(roleId);
        roleMenuMapper.deleteByRoleId(roleId);
        saveMenus(roleId, menuIds);
    }

    private void saveMenus(Long roleId, List<Long> menuIds) {
        if (menuIds == null) {
            return;
        }
        for (Long menuId : menuIds) {
            SysRoleMenu relation = new SysRoleMenu();
            relation.setRoleId(roleId);
            relation.setMenuId(menuId);
            roleMenuMapper.insert(relation);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysRole role = getById(id);
        if ("ADMIN".equals(role.getCode())) {
            throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "内置管理员角色不可删除");
        }
        Long userCount = userRoleMapper.selectCount(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getRoleId, id));
        if (userCount > 0) {
            throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "角色已分配给用户，不能删除");
        }
        roleMapper.deleteById(id);
        roleMenuMapper.deleteByRoleId(id);
    }
}
