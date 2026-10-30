package com.erp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 认证与 RBAC 集成测试：真实登录、真实 JWT、真实接口鉴权。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthRbacIntegrationTest {

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ObjectMapper objectMapper;

    private JsonNode postJson(String path, Object body, String token) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        String payload = objectMapper.writeValueAsString(body);
        ResponseEntity<String> response = rest.exchange(path, HttpMethod.POST,
                new HttpEntity<>(payload, headers), String.class);
        return objectMapper.readTree(response.getBody());
    }

    private ResponseEntity<String> get(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private String login(String username, String password) throws Exception {
        JsonNode body = postJson("/api/auth/login", Map.of("username", username, "password", password), null);
        assertEquals(0, body.get("code").asInt(), "登录应成功: " + body);
        return body.get("data").get("token").asText();
    }

    @Test
    void login_shouldReturnTokenAndProfile() throws Exception {
        JsonNode body = postJson("/api/auth/login", Map.of("username", "admin", "password", "Admin@123"), null);
        assertEquals(0, body.get("code").asInt());
        JsonNode data = body.get("data");
        assertFalse(data.get("token").asText().isBlank());
        assertEquals("admin", data.get("user").get("username").asText());
        assertTrue(data.get("user").get("permissions").size() > 0);
        assertTrue(data.get("user").get("roles").toString().contains("ADMIN"));
    }

    @Test
    void login_withWrongPassword_shouldFailWithBusinessCode() throws Exception {
        JsonNode body = postJson("/api/auth/login", Map.of("username", "admin", "password", "wrong-pass"), null);
        assertEquals(40101, body.get("code").asInt());
        assertNotNull(body.get("message"));
    }

    @Test
    void me_withoutToken_shouldReturn401() {
        ResponseEntity<String> response = get("/api/auth/me", null);
        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    void protectedApi_withSalesToken_shouldBeForbidden() throws Exception {
        String token = login("sales01", "Admin@123");
        ResponseEntity<String> response = get("/api/system/users?page=1&size=10", token);
        assertEquals(403, response.getStatusCode().value());
    }

    @Test
    void protectedApi_withAdminToken_shouldReturnPagedUsers() throws Exception {
        String token = login("admin", "Admin@123");
        ResponseEntity<String> response = get("/api/system/users?page=1&size=10", token);
        assertEquals(200, response.getStatusCode().value());
        JsonNode body = objectMapper.readTree(response.getBody());
        assertEquals(0, body.get("code").asInt());
        assertTrue(body.get("data").get("total").asLong() >= 7);
        assertTrue(body.get("data").get("records").size() > 0);
    }

    @Test
    void menuTree_shouldExposeOnlyGrantedMenus() throws Exception {
        String adminToken = login("admin", "Admin@123");
        JsonNode adminTree = objectMapper.readTree(get("/api/system/menus/tree", adminToken).getBody());
        assertEquals(0, adminTree.get("code").asInt());
        assertTrue(adminTree.get("data").size() >= 6);

        String salesToken = login("sales01", "Admin@123");
        JsonNode salesTree = objectMapper.readTree(get("/api/system/menus/tree", salesToken).getBody());
        assertEquals(0, salesTree.get("code").asInt());
        assertTrue(salesTree.get("data").size() > 0);
        assertTrue(salesTree.get("data").size() < adminTree.get("data").size(),
                "销售专员菜单应少于管理员菜单");
    }
}
