package com.erp;

import com.erp.common.exception.BizException;
import com.erp.common.security.LoginUser;
import com.erp.system.service.IdempotencyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 幂等测试：相同幂等键只允许执行一次业务动作。
 */
@SpringBootTest
class IdempotencyIntegrationTest {

    @Autowired
    private IdempotencyService idempotencyService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String key = "TEST-IDEM-" + System.nanoTime();

    @BeforeEach
    void setUp() {
        LoginUser admin = new LoginUser();
        admin.setUserId(1L);
        admin.setUsername("admin");
        admin.setRoleCodes(new HashSet<>(Set.of("ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM sys_idempotency WHERE idem_key = ?", key);
        SecurityContextHolder.clearContext();
    }

    @Test
    void duplicateKey_shouldBeRejected() {
        AtomicInteger executed = new AtomicInteger();
        String first = idempotencyService.execute(key, "TEST", () -> {
            executed.incrementAndGet();
            return "ok";
        });
        assertEquals("ok", first);

        BizException ex = assertThrows(BizException.class,
                () -> idempotencyService.execute(key, "TEST", () -> {
                    executed.incrementAndGet();
                    return "again";
                }));
        assertEquals(40903, ex.getCode());
        assertEquals(1, executed.get(), "重复请求不应执行业务动作");
    }
}
