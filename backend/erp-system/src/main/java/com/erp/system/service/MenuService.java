package com.erp.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.api.ErrorCode;
import com.erp.common.exception.BizException;
import com.erp.common.security.LoginUser;
import com.erp.common.security.SecurityUtils;
import com.erp.system.dto.MenuNode;
import com.erp.system.entity.SysMenu;
import com.erp.system.mapper.SysMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final SysMenuMapper menuMapper;

    public List<SysMenu> listAll() {
        return menuMapper.selectList(new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSort));
    }

    public List<MenuNode> treeAll() {
        return buildTree(listAll());
    }

    /**
     * 当前登录用户可见菜单；管理员返回全部可见菜单。
     */
    public List<MenuNode> treeOfCurrentUser() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        List<SysMenu> menus;
        if (loginUser.getRoleCodes().contains("ADMIN")) {
            menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                    .in(SysMenu::getType, "DIR", "MENU")
                    .eq(SysMenu::getVisible, 1)
                    .eq(SysMenu::getStatus, 1)
                    .orderByAsc(SysMenu::getSort));
        } else {
            menus = menuMapper.selectVisibleByUserId(loginUser.getUserId());
        }
        return buildTree(menus);
    }

    public List<MenuNode> buildTree(List<SysMenu> menus) {
        Map<Long, MenuNode> nodeMap = menus.stream()
                .collect(Collectors.toMap(SysMenu::getId, MenuNode::from));
        List<MenuNode> roots = new ArrayList<>();
        for (SysMenu menu : menus) {
            MenuNode node = nodeMap.get(menu.getId());
            if (menu.getParentId() == null || menu.getParentId() == 0 || !nodeMap.containsKey(menu.getParentId())) {
                roots.add(node);
            } else {
                nodeMap.get(menu.getParentId()).getChildren().add(node);
            }
        }
        sort(roots);
        return roots;
    }

    private void sort(List<MenuNode> nodes) {
        nodes.sort(Comparator.comparing(n -> n.getSort() == null ? 0 : n.getSort()));
        nodes.forEach(n -> sort(n.getChildren()));
    }

    public Long create(SysMenu menu) {
        menuMapper.insert(menu);
        return menu.getId();
    }

    public void update(Long id, SysMenu menu) {
        SysMenu exists = menuMapper.selectById(id);
        if (exists == null) {
            throw BizException.notFound("菜单不存在");
        }
        menu.setId(id);
        menuMapper.updateById(menu);
    }

    public void delete(Long id) {
        Long children = menuMapper.selectCount(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (children > 0) {
            throw new BizException(ErrorCode.BIZ_RULE_VIOLATION, "请先删除子菜单");
        }
        menuMapper.deleteById(id);
    }
}
