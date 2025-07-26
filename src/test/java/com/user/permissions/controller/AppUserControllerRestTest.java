package com.user.permissions.controller;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
public class AppUserControllerRestTest {
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
        baseUrl = "http://localhost:" + port + "/prueba-tecnica/api";
        
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
    
    // ============ LIST USERS TESTS ============
    @Test
    public void testListUsersAsAdmin() {
        String sessionId = loginAndGetSessionId("admin@example.com", "admin123");
        
        ResponseEntity<Map> response = makeGetRequestWithSession("/users/list", sessionId, Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        
        List<Map<String, Object>> users = (List<Map<String, Object>>) body.get("users");
        assertEquals(2, users.size()); // Should have 2 users (admin and test)
        assertEquals(2, (Integer) body.get("totalCount"));
        
        // Verify user data structure
        Map<String, Object> firstUser = users.get(0);
        assertNotNull(firstUser.get("id"));
        assertNotNull(firstUser.get("email"));
        assertNotNull(firstUser.get("name"));
        assertNotNull(firstUser.get("role"));
        assertNull(firstUser.get("password")); // Password should not be returned
    }

    @Test
    public void testListUsersAsRegularUser() {
        String sessionId = loginAndGetSessionId("test@example.com", "password123");
        
        ResponseEntity<Map> response = makeGetRequestWithSession("/users/list", sessionId, Map.class);
        
        // This should work now because the ADMIN check is commented out in your controller
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
    }

    @Test
    public void testListUsersNotAuthenticated() {
        ResponseEntity<Map> response = makeGetRequest("/users/list", Map.class);
        
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("Not authenticated", body.get("error"));
    }

    // ============ SEARCH USERS TESTS ============
    @Test
    public void testSearchUsersByEmail() {
        String sessionId = loginAndGetSessionId("admin@example.com", "admin123");
        
        ResponseEntity<Map> response = makeGetRequestWithSession("/users/search?email=admin", sessionId, Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        
        List<Map<String, Object>> users = (List<Map<String, Object>>) body.get("users");
        assertEquals(1, users.size());
        assertEquals("admin@example.com", users.get(0).get("email"));
        
        Map<String, Object> searchCriteria = (Map<String, Object>) body.get("searchCriteria");
        assertEquals("admin", searchCriteria.get("email"));
    }

    @Test
    public void testSearchUsersByName() {
        String sessionId = loginAndGetSessionId("admin@example.com", "admin123");
        
        ResponseEntity<Map> response = makeGetRequestWithSession("/users/search?name=Test", sessionId, Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        List<Map<String, Object>> users = (List<Map<String, Object>>) body.get("users");
        assertEquals(1, users.size());
        assertEquals("Test User", users.get(0).get("name"));
    }

    @Test
    public void testSearchUsersByRole() {
        String sessionId = loginAndGetSessionId("admin@example.com", "admin123");
        
        ResponseEntity<Map> response = makeGetRequestWithSession("/users/search?role=ADMIN", sessionId, Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        List<Map<String, Object>> users = (List<Map<String, Object>>) body.get("users");
        assertEquals(1, users.size());
        assertEquals("ADMIN", users.get(0).get("role"));
    }

    @Test
    public void testSearchUsersMultipleFilters() {
        String sessionId = loginAndGetSessionId("admin@example.com", "admin123");
        
        ResponseEntity<Map> response = makeGetRequestWithSession("/users/search?name=Admin&role=ADMIN", sessionId, Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        List<Map<String, Object>> users = (List<Map<String, Object>>) body.get("users");
        assertEquals(1, users.size());
        assertEquals("Admin User", users.get(0).get("name"));
        assertEquals("ADMIN", users.get(0).get("role"));
    }

    @Test
    public void testSearchUsersNoResults() {
        String sessionId = loginAndGetSessionId("admin@example.com", "admin123");
        
        ResponseEntity<Map> response = makeGetRequestWithSession("/users/search?email=nonexistent", sessionId, Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        List<Map<String, Object>> users = (List<Map<String, Object>>) body.get("users");
        assertEquals(0, users.size());
        assertEquals(0, (Integer) body.get("totalCount"));
    }

    @Test
    public void testSearchUsersAsRegularUser() {
        String sessionId = loginAndGetSessionId("test@example.com", "password123");
        
        ResponseEntity<Map> response = makeGetRequestWithSession("/users/search?email=admin", sessionId, Map.class);
        
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertEquals("Access denied. Admin privileges required.", body.get("error"));
    }

    // ============ GET USER BY ID TESTS ============
    @Test
    public void testGetUserByIdAsAdmin() {
        String sessionId = loginAndGetSessionId("admin@example.com", "admin123");
        
        // FIXED: Use the correct endpoint path
        ResponseEntity<Map> response = makeGetRequestWithSession("/users/get/" + testUser.getId(), sessionId, Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        
        Map<String, Object> user = (Map<String, Object>) body.get("user");
        assertEquals(testUser.getEmail(), user.get("email"));
        assertEquals(testUser.getName(), user.get("name"));
        assertEquals(testUser.getRole(), user.get("role"));
        assertNull(user.get("password"));
    }

    @Test
    public void testGetOwnUserProfile() {
        String sessionId = loginAndGetSessionId("test@example.com", "password123");
        
        ResponseEntity<Map> response = makeGetRequestWithSession("/users/get/" + testUser.getId(), sessionId, Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertTrue((Boolean) body.get("success"));
        
        Map<String, Object> user = (Map<String, Object>) body.get("user");
        assertEquals("test@example.com", user.get("email"));
        assertEquals("Test User", user.get("name"));
    }

    @Test
    public void testGetOtherUserProfileAsRegularUser() {
        String sessionId = loginAndGetSessionId("test@example.com", "password123");
        
        // FIXED: Use the correct endpoint path
        ResponseEntity<Map> response = makeGetRequestWithSession("/users/get/" + adminUser.getId(), sessionId, Map.class);
        
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertEquals("Access denied. You can only view your own profile.", body.get("error"));
    }

    @Test
    public void testGetUserByIdNotFound() {
        String sessionId = loginAndGetSessionId("admin@example.com", "admin123");
        
        // FIXED: Use the correct endpoint path
        ResponseEntity<Map> response = makeGetRequestWithSession("/users/get/999", sessionId, Map.class);
        
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertFalse((Boolean) body.get("success"));
        assertEquals("User not found", body.get("error"));
    }

    @Test
    public void testGetUserByIdNotAuthenticated() {
        // FIXED: Use the correct endpoint path
        ResponseEntity<Map> response = makeGetRequest("/users/get/" + testUser.getId(), Map.class);
        
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        
        Map<String, Object> body = response.getBody();
        assertEquals("Not authenticated", body.get("error"));
    }

    // ============ HELPER METHODS ============
    private String loginAndGetSessionId(String email, String password) {
        Map<String, String> loginRequest = Map.of("email", email, "password", password);
        ResponseEntity<Map> response = makePostRequest("/auth/login", loginRequest, Map.class);
        
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
    
 // Add these test methods to your existing AppUserControllerRestTest class

 // ============ CREATE USER TESTS ============
 @Test
 public void testCreateUserAsAdmin() {
     String sessionId = loginAndGetSessionId("admin@example.com", "admin123");
     
     Map<String, String> newUser = Map.of(
         "email", "newuser@example.com",
         "password", "newpassword123",
         "name", "New User",
         "role", "USER"
     );
     
     ResponseEntity<Map> response = makePostRequestWithSession("/users/create", newUser, sessionId, Map.class);
     
     assertEquals(HttpStatus.CREATED, response.getStatusCode());
     
     Map<String, Object> body = response.getBody();
     assertNotNull(body);
     assertTrue((Boolean) body.get("success"));
     assertEquals("User created successfully", body.get("message"));
     
     Map<String, Object> createdUser = (Map<String, Object>) body.get("user");
     assertEquals("newuser@example.com", createdUser.get("email"));
     assertEquals("New User", createdUser.get("name"));
     assertEquals("USER", createdUser.get("role"));
     assertNotNull(createdUser.get("id"));
     assertNotNull(createdUser.get("createdAt"));
     assertNotNull(createdUser.get("updatedAt"));
     assertNull(createdUser.get("password")); // Password should not be returned
     
     // Verify user was actually saved in database
     Optional<AppUser> savedUser = userRepository.findByEmail("newuser@example.com");
     assertTrue(savedUser.isPresent());
     assertEquals("New User", savedUser.get().getName());
 }

 @Test
 public void testCreateUserAsRegularUser() {
     String sessionId = loginAndGetSessionId("test@example.com", "password123");
     
     Map<String, String> newUser = Map.of(
         "email", "newuser@example.com",
         "password", "newpassword123",
         "name", "New User",
         "role", "USER"
     );
     
     ResponseEntity<Map> response = makePostRequestWithSession("/users/create", newUser, sessionId, Map.class);
     
     assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
     
     Map<String, Object> body = response.getBody();
     assertEquals("Access denied. Admin privileges required.", body.get("error"));
 }

 @Test
 public void testCreateUserNotAuthenticated() {
     Map<String, String> newUser = Map.of(
         "email", "newuser@example.com",
         "password", "newpassword123",
         "name", "New User",
         "role", "USER"
     );
     
     ResponseEntity<Map> response = makePostRequest("/users/create", newUser, Map.class);
     
     assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
     
     Map<String, Object> body = response.getBody();
     assertEquals("Not authenticated", body.get("error"));
 }

 @Test
 public void testCreateUserMissingEmail() {
     String sessionId = loginAndGetSessionId("admin@example.com", "admin123");
     
     Map<String, String> newUser = Map.of(
         "password", "newpassword123",
         "name", "New User",
         "role", "USER"
     );
     
     ResponseEntity<Map> response = makePostRequestWithSession("/users/create", newUser, sessionId, Map.class);
     
     assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
     
     Map<String, Object> body = response.getBody();
     assertEquals("Email is required", body.get("error"));
 }

 @Test
 public void testCreateUserMissingPassword() {
     String sessionId = loginAndGetSessionId("admin@example.com", "admin123");
     
     Map<String, String> newUser = Map.of(
         "email", "newuser@example.com",
         "name", "New User",
         "role", "USER"
     );
     
     ResponseEntity<Map> response = makePostRequestWithSession("/users/create", newUser, sessionId, Map.class);
     
     assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
     
     Map<String, Object> body = response.getBody();
     assertEquals("Password is required", body.get("error"));
 }

 @Test
 public void testCreateUserMissingName() {
     String sessionId = loginAndGetSessionId("admin@example.com", "admin123");
     
     Map<String, String> newUser = Map.of(
         "email", "newuser@example.com",
         "password", "newpassword123",
         "role", "USER"
     );
     
     ResponseEntity<Map> response = makePostRequestWithSession("/users/create", newUser, sessionId, Map.class);
     
     assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
     
     Map<String, Object> body = response.getBody();
     assertEquals("Name is required", body.get("error"));
 }

 @Test
 public void testCreateUserMissingRole() {
     String sessionId = loginAndGetSessionId("admin@example.com", "admin123");
     
     Map<String, String> newUser = Map.of(
         "email", "newuser@example.com",
         "password", "newpassword123",
         "name", "New User"
     );
     
     ResponseEntity<Map> response = makePostRequestWithSession("/users/create", newUser, sessionId, Map.class);
     
     assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
     
     Map<String, Object> body = response.getBody();
     assertEquals("Role is required", body.get("error"));
 }

 @Test
 public void testCreateUserInvalidRole() {
     String sessionId = loginAndGetSessionId("admin@example.com", "admin123");
     
     Map<String, String> newUser = Map.of(
         "email", "newuser@example.com",
         "password", "newpassword123",
         "name", "New User",
         "role", "INVALID_ROLE"
     );
     
     ResponseEntity<Map> response = makePostRequestWithSession("/users/create", newUser, sessionId, Map.class);
     
     assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
     
     Map<String, Object> body = response.getBody();
     assertEquals("Role must be either USER or ADMIN", body.get("error"));
 }

 @Test
 public void testCreateUserDuplicateEmail() {
     String sessionId = loginAndGetSessionId("admin@example.com", "admin123");
     
     Map<String, String> newUser = Map.of(
         "email", "test@example.com", // This email already exists
         "password", "newpassword123",
         "name", "New User",
         "role", "USER"
     );
     
     ResponseEntity<Map> response = makePostRequestWithSession("/users/create", newUser, sessionId, Map.class);
     
     assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
     
     Map<String, Object> body = response.getBody();
     assertEquals("Email already exists", body.get("error"));
 }

 // ============ UPDATE USER TESTS ============
 @Test
 public void testUpdateUserAsAdmin() {
     String sessionId = loginAndGetSessionId("admin@example.com", "admin123");
     
     Map<String, String> updateData = Map.of(
         "name", "Updated Test User",
         "role", "ADMIN"
     );
     
     ResponseEntity<Map> response = makePutRequestWithSession("/users/update/" + testUser.getId(), updateData, sessionId, Map.class);
     
     assertEquals(HttpStatus.OK, response.getStatusCode());
     
     Map<String, Object> body = response.getBody();
     assertNotNull(body);
     assertTrue((Boolean) body.get("success"));
     assertEquals("User updated successfully", body.get("message"));
     
     Map<String, Object> updatedUser = (Map<String, Object>) body.get("user");
     assertEquals("Updated Test User", updatedUser.get("name"));
     assertEquals("ADMIN", updatedUser.get("role"));
     assertEquals(testUser.getEmail(), updatedUser.get("email"));
     
     // Verify user was actually updated in database
     Optional<AppUser> savedUser = userRepository.findById(testUser.getId());
     assertTrue(savedUser.isPresent());
     assertEquals("Updated Test User", savedUser.get().getName());
     assertEquals("ADMIN", savedUser.get().getRole());
 }

 @Test
 public void testUpdateOwnProfile() {
     String sessionId = loginAndGetSessionId("test@example.com", "password123");
     
     Map<String, String> updateData = Map.of(
         "name", "Updated Name",
         "email", "updated@example.com"
     );
     
     ResponseEntity<Map> response = makePutRequestWithSession("/users/update/" + testUser.getId(), updateData, sessionId, Map.class);
     
     assertEquals(HttpStatus.OK, response.getStatusCode());
     
     Map<String, Object> body = response.getBody();
     assertTrue((Boolean) body.get("success"));
     
     Map<String, Object> updatedUser = (Map<String, Object>) body.get("user");
     assertEquals("Updated Name", updatedUser.get("name"));
     assertEquals("updated@example.com", updatedUser.get("email"));
 }

 @Test
 public void testUpdateOtherUserProfileAsRegularUser() {
     String sessionId = loginAndGetSessionId("test@example.com", "password123");
     
     Map<String, String> updateData = Map.of(
         "name", "Hacked Name"
     );
     
     ResponseEntity<Map> response = makePutRequestWithSession("/users/update/" + adminUser.getId(), updateData, sessionId, Map.class);
     
     assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
     
     Map<String, Object> body = response.getBody();
     assertEquals("Access denied. You can only update your own profile.", body.get("error"));
 }

 @Test
 public void testUpdateUserRoleAsRegularUser() {
     String sessionId = loginAndGetSessionId("test@example.com", "password123");
     
     Map<String, String> updateData = Map.of(
         "role", "ADMIN"
     );
     
     ResponseEntity<Map> response = makePutRequestWithSession("/users/update/" + testUser.getId(), updateData, sessionId, Map.class);
     
     assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
     
     Map<String, Object> body = response.getBody();
     assertEquals("Only admins can update user roles", body.get("error"));
 }

 @Test
 public void testUpdateUserNotFound() {
     String sessionId = loginAndGetSessionId("admin@example.com", "admin123");
     
     Map<String, String> updateData = Map.of(
         "name", "Updated Name"
     );
     
     ResponseEntity<Map> response = makePutRequestWithSession("/users/update/999", updateData, sessionId, Map.class);
     
     assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
     
     Map<String, Object> body = response.getBody();
     assertFalse((Boolean) body.get("success"));
     assertEquals("User not found", body.get("error"));
 }

 @Test
 public void testUpdateUserDuplicateEmail() {
     String sessionId = loginAndGetSessionId("admin@example.com", "admin123");
     
     Map<String, String> updateData = Map.of(
         "email", "admin@example.com" // This email is already taken by admin
     );
     
     ResponseEntity<Map> response = makePutRequestWithSession("/users/update/" + testUser.getId(), updateData, sessionId, Map.class);
     
     assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
     
     Map<String, Object> body = response.getBody();
     assertEquals("Email already exists", body.get("error"));
 }

 @Test
 public void testUpdateUserInvalidRole() {
     String sessionId = loginAndGetSessionId("admin@example.com", "admin123");
     
     Map<String, String> updateData = Map.of(
         "role", "INVALID"
     );
     
     ResponseEntity<Map> response = makePutRequestWithSession("/users/update/" + testUser.getId(), updateData, sessionId, Map.class);
     
     assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
     
     Map<String, Object> body = response.getBody();
     assertEquals("Role must be either USER or ADMIN", body.get("error"));
 }

 @Test
 public void testUpdateUserNoFields() {
     String sessionId = loginAndGetSessionId("admin@example.com", "admin123");
     
     Map<String, String> updateData = Map.of(); // Empty update
     
     ResponseEntity<Map> response = makePutRequestWithSession("/users/update/" + testUser.getId(), updateData, sessionId, Map.class);
     
     assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
     
     Map<String, Object> body = response.getBody();
     assertEquals("No valid fields provided for update", body.get("error"));
 }

 @Test
 public void testUpdateUserPassword() {
     String sessionId = loginAndGetSessionId("admin@example.com", "admin123");
     
     Map<String, String> updateData = Map.of(
         "password", "newpassword123"
     );
     
     ResponseEntity<Map> response = makePutRequestWithSession("/users/update/" + testUser.getId(), updateData, sessionId, Map.class);
     
     assertEquals(HttpStatus.OK, response.getStatusCode());
     
     Map<String, Object> body = response.getBody();
     assertTrue((Boolean) body.get("success"));
     
     // Verify password was updated by trying to login with new password
     Map<String, String> loginRequest = Map.of("email", "test@example.com", "password", "newpassword123");
     ResponseEntity<Map> loginResponse = makePostRequest("/auth/login", loginRequest, Map.class);
     assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
 }

 @Test
 public void testUpdateUserNotAuthenticated() {
     Map<String, String> updateData = Map.of(
         "name", "Updated Name"
     );
     
     ResponseEntity<Map> response = makePutRequest("/users/update/" + testUser.getId(), updateData, Map.class);
     
     assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
     
     Map<String, Object> body = response.getBody();
     assertEquals("Not authenticated", body.get("error"));
 }

 // ============ HELPER METHODS FOR CREATE/UPDATE TESTS ============
 private ResponseEntity<Map> makePostRequestWithSession(String endpoint, Object body, String sessionId, Class<Map> responseType) {
     HttpHeaders headers = new HttpHeaders();
     headers.setContentType(MediaType.APPLICATION_JSON);
     if (sessionId != null) {
         headers.set("Cookie", sessionId);
     }
     HttpEntity<Object> request = new HttpEntity<>(body, headers);
     
     return restTemplate.postForEntity(baseUrl + endpoint, request, responseType);
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

 private ResponseEntity<Map> makePutRequest(String endpoint, Object body, Class<Map> responseType) {
     HttpHeaders headers = new HttpHeaders();
     headers.setContentType(MediaType.APPLICATION_JSON);
     HttpEntity<Object> request = new HttpEntity<>(body, headers);
     
     return restTemplate.exchange(baseUrl + endpoint, HttpMethod.PUT, request, responseType);
 }
 
 @Test
 public void testDeleteUserAsAdmin() {
     String sessionId = loginAndGetSessionId("admin@example.com", "admin123");
     
     // Create a new user to delete (so we don't delete test data)
     AppUser userToDelete = new AppUser();
     userToDelete.setEmail("todelete@example.com");
     userToDelete.setPassword(passwordEncoder.encode("password123"));
     userToDelete.setName("User To Delete");
     userToDelete.setRole("USER");
     userToDelete = userRepository.save(userToDelete);
     
     ResponseEntity<Map> response = makeDeleteRequestWithSession("/users/delete/" + userToDelete.getId(), sessionId, Map.class);
     
     assertEquals(HttpStatus.OK, response.getStatusCode());
     
     Map<String, Object> body = response.getBody();
     assertNotNull(body);
     assertTrue((Boolean) body.get("success"));
     assertEquals("User deleted successfully", body.get("message"));
     
     Map<String, Object> deletedUser = (Map<String, Object>) body.get("deletedUser");
     assertEquals("todelete@example.com", deletedUser.get("email"));
     assertEquals("User To Delete", deletedUser.get("name"));
     assertEquals("USER", deletedUser.get("role"));
     
     // Verify user was actually deleted from database
     Optional<AppUser> deletedFromDb = userRepository.findById(userToDelete.getId());
     assertFalse(deletedFromDb.isPresent());
 }

 @Test
 public void testDeleteUserAsRegularUser() {
     String sessionId = loginAndGetSessionId("test@example.com", "password123");
     
     ResponseEntity<Map> response = makeDeleteRequestWithSession("/users/delete/" + adminUser.getId(), sessionId, Map.class);
     
     assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
     
     Map<String, Object> body = response.getBody();
     assertEquals("Access denied. Admin privileges required.", body.get("error"));
 }

 @Test
 public void testDeleteUserNotAuthenticated() {
     ResponseEntity<Map> response = makeDeleteRequest("/users/delete/" + testUser.getId(), Map.class);
     
     assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
     
     Map<String, Object> body = response.getBody();
     assertEquals("Not authenticated", body.get("error"));
 }

 @Test
 public void testDeleteOwnAccount() {
     String sessionId = loginAndGetSessionId("admin@example.com", "admin123");
     
     ResponseEntity<Map> response = makeDeleteRequestWithSession("/users/delete/" + adminUser.getId(), sessionId, Map.class);
     
     assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
     
     Map<String, Object> body = response.getBody();
     assertEquals("You cannot delete your own account", body.get("error"));
     
     // Verify admin user still exists
     Optional<AppUser> adminStillExists = userRepository.findById(adminUser.getId());
     assertTrue(adminStillExists.isPresent());
 }

 @Test
 public void testDeleteUserNotFound() {
     String sessionId = loginAndGetSessionId("admin@example.com", "admin123");
     
     ResponseEntity<Map> response = makeDeleteRequestWithSession("/users/delete/999", sessionId, Map.class);
     
     assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
     
     Map<String, Object> body = response.getBody();
     assertFalse((Boolean) body.get("success"));
     assertEquals("User not found", body.get("error"));
 }

   @Test
   public void testDeleteUserAndVerifyListUpdated() {
       String sessionId = loginAndGetSessionId("admin@example.com", "admin123");
       
       // Create a new user to delete
       AppUser userToDelete = new AppUser();
       userToDelete.setEmail("todelete2@example.com");
       userToDelete.setPassword(passwordEncoder.encode("password123"));
       userToDelete.setName("User To Delete 2");
       userToDelete.setRole("USER");
       userToDelete = userRepository.save(userToDelete);
       
       // Verify user exists in list initially (should have 3 users: admin, test, new)
       ResponseEntity<Map> listResponse = makeGetRequestWithSession("/users/list", sessionId, Map.class);
       assertEquals(HttpStatus.OK, listResponse.getStatusCode());
       List<Map<String, Object>> usersBeforeDelete = (List<Map<String, Object>>) listResponse.getBody().get("users");
       assertEquals(3, usersBeforeDelete.size());
       
       // Delete the user
       ResponseEntity<Map> deleteResponse = makeDeleteRequestWithSession("/users/delete/" + userToDelete.getId(), sessionId, Map.class);
       assertEquals(HttpStatus.OK, deleteResponse.getStatusCode());
       
       // Verify user is removed from list (should have 2 users: admin, test)
       ResponseEntity<Map> listAfterDelete = makeGetRequestWithSession("/users/list", sessionId, Map.class);
       assertEquals(HttpStatus.OK, listAfterDelete.getStatusCode());
       List<Map<String, Object>> usersAfterDelete = (List<Map<String, Object>>) listAfterDelete.getBody().get("users");
       assertEquals(2, usersAfterDelete.size());
       
       // Verify the deleted user is not in the list
       boolean userFoundInList = usersAfterDelete.stream()
           .anyMatch(user -> "todelete2@example.com".equals(user.get("email")));
       assertFalse(userFoundInList);
   }
 
   //============ ADDITIONAL HELPER METHODS ============
   private ResponseEntity<Map> makeDeleteRequestWithSession(String endpoint, String sessionId, Class<Map> responseType) {
     HttpHeaders headers = new HttpHeaders();
     if (sessionId != null) {
         headers.set("Cookie", sessionId);
     }
     HttpEntity<?> request = new HttpEntity<>(headers);
     
     return restTemplate.exchange(baseUrl + endpoint, HttpMethod.DELETE, request, responseType);
   }
   
   private ResponseEntity<Map> makeDeleteRequest(String endpoint, Class<Map> responseType) {
     return restTemplate.exchange(baseUrl + endpoint, HttpMethod.DELETE, null, responseType);
   }
}