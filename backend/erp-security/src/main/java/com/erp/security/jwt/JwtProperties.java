package com.erp.security.jwt;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "erp.jwt")
public class JwtProperties {

    /** Base64 或明文字符串，长度至少 32 字节 */
    private String secret = "erp-system-jwt-secret-key-please-change-in-production-2026";
    private long expireMinutes = 480;
}
