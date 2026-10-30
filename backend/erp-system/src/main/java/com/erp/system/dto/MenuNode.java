package com.erp.system.dto;

import com.erp.system.entity.SysMenu;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树节点。
 */
@Data
public class MenuNode {

    private Long id;
    private Long parentId;
    private String name;
    private String type;
    private String path;
    private String component;
    private String permission;
    private String icon;
    private Integer sort;
    private Integer visible;
    private Integer status;
    private List<MenuNode> children = new ArrayList<>();

    public static MenuNode from(SysMenu menu) {
        MenuNode node = new MenuNode();
        node.setId(menu.getId());
        node.setParentId(menu.getParentId());
        node.setName(menu.getName());
        node.setType(menu.getType());
        node.setPath(menu.getPath());
        node.setComponent(menu.getComponent());
        node.setPermission(menu.getPermission());
        node.setIcon(menu.getIcon());
        node.setSort(menu.getSort());
        node.setVisible(menu.getVisible());
        node.setStatus(menu.getStatus());
        return node;
    }
}
