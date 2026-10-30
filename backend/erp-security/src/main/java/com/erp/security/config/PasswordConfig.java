package com.erp.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 独立于 SecurityConfig 的密码编码器配置。
 * 单独拆出以避免 userService（依赖 PasswordEncoder）与 securityConfig（依赖 JwtAuthenticationFilter -> userService）形成循环依赖。
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
