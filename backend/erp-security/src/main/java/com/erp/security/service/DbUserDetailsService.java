package com.erp.security.service;

import com.erp.common.security.LoginUser;
import com.erp.system.entity.SysUser;
import com.erp.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 数据库用户认证。
 */
@Service
@RequiredArgsConstructor
public class DbUserDetailsService implements UserDetailsService {

    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userService.getByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new UsernameNotFoundException("账号已停用");
        }
        LoginUser loginUser = userService.buildLoginUser(user);
        if (!loginUser.isEnabled()) {
            throw new UsernameNotFoundException("账号已停用");
        }
        return loginUser;
    }
}
