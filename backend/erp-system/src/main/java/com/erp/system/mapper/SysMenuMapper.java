package com.erp.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.system.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    @Select("""
            SELECT DISTINCT m.permission FROM sys_menu m
            JOIN sys_role_menu rm ON m.id = rm.menu_id
            JOIN sys_user_role ur ON rm.role_id = ur.role_id
            JOIN sys_role r ON r.id = ur.role_id AND r.status = 1 AND r.deleted = 0
            WHERE ur.user_id = #{userId} AND m.permission IS NOT NULL AND m.permission <> ''
              AND m.status = 1 AND m.deleted = 0
            """)
    List<String> selectPermissionsByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT DISTINCT m.* FROM sys_menu m
            JOIN sys_role_menu rm ON m.id = rm.menu_id
            JOIN sys_user_role ur ON rm.role_id = ur.role_id
            JOIN sys_role r ON r.id = ur.role_id AND r.status = 1 AND r.deleted = 0
            WHERE ur.user_id = #{userId} AND m.type IN ('DIR','MENU')
              AND m.visible = 1 AND m.status = 1 AND m.deleted = 0
            ORDER BY m.sort, m.id
            """)
    List<SysMenu> selectVisibleByUserId(@Param("userId") Long userId);
}
