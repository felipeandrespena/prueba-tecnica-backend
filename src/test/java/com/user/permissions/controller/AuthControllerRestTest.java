package com.user.permissions.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;

import com.user.permissions.appuser.AppUser;
import com.user.permissions.appuser.repository.AppUserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AuthControllerRestTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String baseUrl;
    private AppUser testUser;
    private AppUser adminUser;

    @BeforeEach
    public void setUp() {
        baseUrl = "http://localhost:" + port + "/prueba-tecnica/api/auth";
        
        // Clear existing users
        userRepository.deleteAll();
        
        // Create test users
        testUser = new AppUser();
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setName("Test User");
        testUser.setRole("USER");
        testUser = userRepository.save(testUser);

        adminUser = new AppUser();
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword(passwordEncoder.encode("admin123"));
        adminUser.setName("Admin User");
        adminUser.setRole("ADMIN");
        adminUser = userRepository.save(adminUser);
        
        System.out.println("Created test users:");
        System.out.println("- " + testUser.getEmail() + " (ID: " + testUser.getId() + ")");
        System.out.println("- " + adminUser.getEmail() + " (ID: " + adminUser.getId() + ")");
    }

    // ============ LOGIN TESTS ============

    @Test
    public void testLoginSuccess() {
        Map<String, String> loginRequest = Map.of(
            "email", "admin@example.com",
            "password", "admin123"
        );

        ResponseEntity<Map> response = makePostRequest("/login", loginRequest, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        
        Map<String, Object> user = (Map<String, Object>) body.get("user");
        assertEquals("admin@example.com", user.get("email"));
        assertEquals("Admin User", user.get("name"));
        assertEquals("ADMIN", user.get("role"));
        assertNull(user.get("password")); // Password should not be returned
    }

    @Test
    public void testLoginWithTestUser() {
        Map<String, String> loginRequest = Map.of(
            "email", "test@example.com",
            "password", "password123"
        );

        ResponseEntity<Map> response = makePostRequest("/login", loginRequest, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        
        Map<String, Object> user = (Map<String, Object>) body.get("user");
        assertEquals("test@example.com", user.get("email"));
        assertEquals("Test User", user.get("name"));
        assertEquals("USER", user.get("role"));
    }

    @Test
    public void testLoginWithInvalidEmail() {
        Map<String, String> loginRequest = Map.of(
            "email", "nonexistent@example.com",
            "password", "admin123"
        );

        ResponseEntity<Map> response = makePostRequest("/login", loginRequest, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        assertEquals("Invalid email or password", body.get("error"));
    }

    @Test
    public void testLoginWithInvalidPassword() {
        Map<String, String> loginRequest = Map.of(
            "email", "admin@example.com",
            "password", "wrongpassword"
        );

        ResponseEntity<Map> response = makePostRequest("/login", loginRequest, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        assertEquals("Invalid email or password", body.get("error"));
    }

    @Test
    public void testLoginWithEmptyCredentials() {
        Map<String, String> loginRequest = Map.of(
            "email", "",
            "password", ""
        );

        ResponseEntity<Map> response = makePostRequest("/login", loginRequest, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
    }

    // ============ SESSION TESTS ============

    @Test
    public void testGetCurrentUserWhenNotLoggedIn() {
        ResponseEntity<Map> response = makeGetRequest("/me", Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("authenticated"));
    }

    @Test
    public void testGetCurrentUserWhenLoggedIn() {
        // First login
        String sessionId = loginAndGetSessionId("admin@example.com", "admin123");
        
        // Then check current user
        ResponseEntity<Map> response = makeGetRequestWithSession("/me", sessionId, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("authenticated"));
        
        Map<String, Object> user = (Map<String, Object>) body.get("user");
        assertEquals("admin@example.com", user.get("email"));
        assertEquals("Admin User", user.get("name"));
    }

    @Test
    public void testLogout() {
        // First login
        String sessionId = loginAndGetSessionId("admin@example.com", "admin123");
        
        // Then logout
        ResponseEntity<Map> logoutResponse = makePostRequestWithSession("/logout", null, sessionId, Map.class);
        assertEquals(HttpStatus.OK, logoutResponse.getStatusCode());
        assertTrue((Boolean) logoutResponse.getBody().get("success"));
        
        // Verify session is invalidated
        ResponseEntity<Map> meResponse = makeGetRequestWithSession("/me", sessionId, Map.class);
        assertFalse((Boolean) meResponse.getBody().get("authenticated"));
    }

    // ============ UPDATE USER TESTS ============

    @Test
    public void testUpdateUserSuccess() {
        String sessionId = loginAndGetSessionId("test@example.com", "password123");
        
        Map<String, String> updateRequest = Map.of(
            "name", "Updated Test User",
            "email", "updated@example.com",
            "password", "newPassword123"
        );

        ResponseEntity<Map> response = makePutRequestWithSession("/update", updateRequest, sessionId, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertEquals("User updated successfully", body.get("message"));
        
        Map<String, Object> user = (Map<String, Object>) body.get("user");
        assertEquals("Updated Test User", user.get("name"));
        assertEquals("updated@example.com", user.get("email"));
        assertNull(user.get("password")); // Password should not be returned
    }

    @Test
    public void testUpdateUserNotAuthenticated() {
        Map<String, String> updateRequest = Map.of(
            "name", "Updated Name",
            "email", "updated@example.com",
            "password", "newPassword123"
        );

        ResponseEntity<Map> response = makePutRequest("/update", updateRequest, Map.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("Not authenticated", body.get("error"));
    }

    @Test
    public void testUpdateUserEmptyName() {
        String sessionId = loginAndGetSessionId("test@example.com", "password123");
        
        Map<String, String> updateRequest = Map.of(
            "name", "",
            "email", "test@example.com",
            "password", "password123"
        );

        ResponseEntity<Map> response = makePutRequestWithSession("/update", updateRequest, sessionId, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
        
        Map<String, String> errors = (Map<String, String>) body.get("errors");
        assertEquals("Name is required and cannot be empty", errors.get("name"));
    }

    @Test
    public void testUpdateUserEmptyEmail() {
        String sessionId = loginAndGetSessionId("test@example.com", "password123");
        
        Map<String, String> updateRequest = Map.of(
            "name", "Test User",
            "email", "",
            "password", "password123"
        );

        ResponseEntity<Map> response = makePutRequestWithSession("/update", updateRequest, sessionId, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        Map<String, String> errors = (Map<String, String>) body.get("errors");
        assertEquals("Email is required and cannot be empty", errors.get("email"));
    }

    @Test
    public void testUpdateUserInvalidEmail() {
        String sessionId = loginAndGetSessionId("test@example.com", "password123");
        
        Map<String, String> updateRequest = Map.of(
            "name", "Test User",
            "email", "invalid-email",
            "password", "password123"
        );

        ResponseEntity<Map> response = makePutRequestWithSession("/update", updateRequest, sessionId, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        Map<String, String> errors = (Map<String, String>) body.get("errors");
        assertEquals("Please provide a valid email address", errors.get("email"));
    }

    @Test
    public void testUpdateUserEmptyPassword() {
        String sessionId = loginAndGetSessionId("test@example.com", "password123");
        
        Map<String, String> updateRequest = Map.of(
            "name", "Test User",
            "email", "test@example.com",
            "password", ""
        );

        ResponseEntity<Map> response = makePutRequestWithSession("/update", updateRequest, sessionId, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        Map<String, String> errors = (Map<String, String>) body.get("errors");
        assertEquals("Password is required and cannot be empty", errors.get("password"));
    }

    @Test
    public void testUpdateUserShortPassword() {
        String sessionId = loginAndGetSessionId("test@example.com", "password123");
        
        Map<String, String> updateRequest = Map.of(
            "name", "Test User",
            "email", "test@example.com",
            "password", "123"
        );

        ResponseEntity<Map> response = makePutRequestWithSession("/update", updateRequest, sessionId, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        Map<String, String> errors = (Map<String, String>) body.get("errors");
        assertEquals("Password must be at least 6 characters long", errors.get("password"));
    }

    @Test
    public void testUpdateUserExistingEmail() {
        String sessionId = loginAndGetSessionId("test@example.com", "password123");
        
        // Try to use admin's email
        Map<String, String> updateRequest = Map.of(
            "name", "Test User",
            "email", "admin@example.com", // This email already exists
            "password", "password123"
        );

        ResponseEntity<Map> response = makePutRequestWithSession("/update", updateRequest, sessionId, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        Map<String, String> errors = (Map<String, String>) body.get("errors");
        assertEquals("Email already exists. Please choose a different email.", errors.get("email"));
    }

    @Test
    public void testUpdateUserSameEmailAsCurrentUser() {
        String sessionId = loginAndGetSessionId("test@example.com", "password123");
        
        // Use same email as current user (should be allowed)
        Map<String, String> updateRequest = Map.of(
            "name", "Updated Test User",
            "email", "test@example.com", // Same email as current user
            "password", "newPassword123"
        );

        ResponseEntity<Map> response = makePutRequestWithSession("/update", updateRequest, sessionId, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertTrue((Boolean) body.get("success"));
    }

    @Test
    public void testUpdateUserMultipleValidationErrors() {
        String sessionId = loginAndGetSessionId("test@example.com", "password123");
        
        Map<String, String> updateRequest = Map.of(
            "name", "",                    // Empty name
            "email", "invalid-email",      // Invalid email
            "password", "123"              // Short password
        );

        ResponseEntity<Map> response = makePutRequestWithSession("/update", updateRequest, sessionId, Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertFalse((Boolean) body.get("success"));
        
        Map<String, String> errors = (Map<String, String>) body.get("errors");
        assertEquals(3, errors.size());
        assertNotNull(errors.get("name"));
        assertNotNull(errors.get("email"));
        assertNotNull(errors.get("password"));
    }

    // ============ HELPER METHODS ============

    private String loginAndGetSessionId(String email, String password) {
        Map<String, String> loginRequest = Map.of("email", email, "password", password);
        ResponseEntity<Map> response = makePostRequest("/login", loginRequest, Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // Extract JSESSIONID from Set-Cookie header
        List<String> cookies = response.getHeaders().get("Set-Cookie");
        if (cookies != null) {
            for (String cookie : cookies) {
                if (cookie.startsWith("JSESSIONID=")) {
                    return cookie.split(";")[0]; // Return just "JSESSIONID=value"
                }
            }
        }
        return null;
    }

    private ResponseEntity<Map> makePostRequest(String endpoint, Object body, Class<Map> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> request = new HttpEntity<>(body, headers);
        
        return restTemplate.postForEntity(baseUrl + endpoint, request, responseType);
    }

    private ResponseEntity<Map> makePostRequestWithSession(String endpoint, Object body, String sessionId, Class<Map> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (sessionId != null) {
            headers.set("Cookie", sessionId);
        }
        HttpEntity<Object> request = new HttpEntity<>(body, headers);
        
        return restTemplate.postForEntity(baseUrl + endpoint, request, responseType);
    }

    private ResponseEntity<Map> makeGetRequest(String endpoint, Class<Map> responseType) {
        return restTemplate.getForEntity(baseUrl + endpoint, responseType);
    }

    private ResponseEntity<Map> makeGetRequestWithSession(String endpoint, String sessionId, Class<Map> responseType) {
        HttpHeaders headers = new HttpHeaders();
        if (sessionId != null) {
            headers.set("Cookie", sessionId);
        }
        HttpEntity<?> request = new HttpEntity<>(headers);
        
        return restTemplate.exchange(baseUrl + endpoint, HttpMethod.GET, request, responseType);
    }

    private ResponseEntity<Map> makePutRequest(String endpoint, Object body, Class<Map> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> request = new HttpEntity<>(body, headers);
        
        return restTemplate.exchange(baseUrl + endpoint, HttpMethod.PUT, request, responseType);
    }

    private ResponseEntity<Map> makePutRequestWithSession(String endpoint, Object body, String sessionId, Class<Map> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (sessionId != null) {
            headers.set("Cookie", sessionId);
        }
        HttpEntity<Object> request = new HttpEntity<>(body, headers);
        
        return restTemplate.exchange(baseUrl + endpoint, HttpMethod.PUT, request, responseType);
    }
}