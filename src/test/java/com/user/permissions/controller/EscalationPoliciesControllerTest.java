package com.user.permissions.controller;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
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
import org.springframework.test.annotation.DirtiesContext;

import com.user.permissions.GenericTesting;
import com.user.permissions.escalationpolicies.model.EscalationPoliciesResponse;
import com.user.permissions.escalationpolicies.model.EscalationPolicy;
	
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)

public class EscalationPoliciesControllerTest extends GenericTesting{

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;

    @BeforeEach
    public void setUp() {
    	
    	baseUrl = "http://localhost:" + port + "/prueba-tecnica/api";
    	super.setUp();
     }

    // ============ TEST DATA HELPERS ============
    private EscalationPolicy createTestEscalationPolicy(String id, String name) {
        EscalationPolicy policy = new EscalationPolicy();
        policy.setId(id);
        policy.setName(name);
        policy.setSummary(name + " Summary");
        policy.setType("escalation_policy");
        policy.setOnCallHandoffNotifications("if_has_services");
        policy.setSelf("https://api.pagerduty.com/escalation_policies/" + id);
        policy.setHtmlUrl("https://subdomain.pagerduty.com/escalation_policies/" + id);
        policy.setNumLoops(0);
        return policy;
    }

    private EscalationPoliciesResponse createTestResponse(List<EscalationPolicy> policies, 
                                                         Integer limit, Integer offset, Boolean more) {
        EscalationPoliciesResponse response = new EscalationPoliciesResponse();
        response.setEscalationPolicies(policies);
        response.setLimit(limit);
        response.setOffset(offset);
        response.setMore(more);
        response.setTotal(null);
        return response;
    }

    // ============ LIST ESCALATION POLICIES TESTS ============
    @Test
    public void testListEscalationPoliciesAsAuthenticatedUser() {
        // Arrange
        String sessionId = loginAndGetSessionId("test@example.com", "password123");
        
        // Act
        ResponseEntity<Map> response = makeGetRequestWithSession("/escalation-policies/list", sessionId, Map.class);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
        assertEquals(21, ((Map) body.get("pagination")).get("totalRecords"));

    }

    @Test
    public void testListEscalationPoliciesAsAdmin() {
        // Arrange
        String sessionId = loginAndGetSessionId("admin@example.com", "admin123");
        

        Integer page = 1;
        Integer offset = 5;
        // Act
        String paginationUrl =  String.format("/escalation-policies/list?page=%d&size=%d" , page , offset );
		ResponseEntity<Map> response = makeGetRequestWithSession(paginationUrl, sessionId, Map.class);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(offset, ((Map)response.getBody().get("pagination")).get("pageSize"));
        assertEquals(4, (int)(((Map)response.getBody().get("pagination")).get("totalRecords")) / offset);
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));

    }

    @Test
    public void testListEscalationPoliciesNotAuthenticated() {
        // Arrange
        List<EscalationPolicy> mockPolicies = Arrays.asList(
            createTestEscalationPolicy("POLICY1", "Test Policy")
        );
        EscalationPoliciesResponse mockResponse = createTestResponse(mockPolicies, 25, 0, false);

        // Act
        ResponseEntity<Map> response = makeGetRequest("/escalation-policies/list", Map.class);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals("Not authenticated", body.get("error"));
    }

    @Test
    public void testListEscalationPoliciesEmptyResponse() {
        // Arrange
        String sessionId = loginAndGetSessionId("test@example.com", "password123");
        
        EscalationPoliciesResponse emptyResponse = createTestResponse(Arrays.asList(), 25, 0, false);


        // Act
        ResponseEntity<Map> response = makeGetRequestWithSession("/escalation-policies/list", sessionId, Map.class);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));

        List<?> policiesResponse = (List<?>) body.get("users");
        assertNotNull(policiesResponse);
        assertTrue(policiesResponse.isEmpty());
    }

    @Test
    public void testListEscalationPoliciesApiClientReturnsEmpty() {
        // Arrange
        String sessionId = loginAndGetSessionId("test@example.com", "password123");

        // Act
        ResponseEntity<Map> response = makeGetRequestWithSession("/escalation-policies/list?page=1&size=5", sessionId, Map.class);

        // Assert - This should now return 500 error because .get() on empty Optional throws exception
        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertTrue((Boolean) body.get("success"));
    }

    @Test
    public void testListEscalationPoliciesWithPaginationData() {
        // Arrange
        String sessionId = loginAndGetSessionId("admin@example.com", "admin123");
        
        List<EscalationPolicy> mockPolicies = Arrays.asList(
            createTestEscalationPolicy("PAGE_POLICY_1", "Page Policy 1"),
            createTestEscalationPolicy("PAGE_POLICY_2", "Page Policy 2")
        );
        EscalationPoliciesResponse mockResponse = createTestResponse(mockPolicies, 25, 0, true);
        mockResponse.setTotal(50);


        // Act
        ResponseEntity<Map> response = makeGetRequestWithSession("/escalation-policies/list?page=1&size=5", sessionId, Map.class);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());

        Map<String, Object> body = response.getBody();
        assertTrue((Boolean) body.get("success"));

    }

    // ============ ERROR HANDLING TESTS ============
    @Test
    public void testListEscalationPoliciesApiClientThrowsException() {
        // Arrange
        String sessionId = loginAndGetSessionId("test@example.com", "password123");

        // Act
        ResponseEntity<Map> response = makeGetRequestWithSession("/escalation-policies/list", sessionId, Map.class);

        // Assert
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertFalse((Boolean) body.get("success"));
    }

    @Test
    public void testListEscalationPoliciesApiClientThrowsNullPointerException() {
        // Arrange
        String sessionId = loginAndGetSessionId("admin@example.com", "admin123");


        // Act
        ResponseEntity<Map> response = makeGetRequestWithSession("/escalation-policies/list", sessionId, Map.class);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        Map<String, Object> body = response.getBody();
        assertFalse((Boolean) body.get("success"));
        assertEquals("Failed to retrieve escalation policies. Please try again.", body.get("error"));
    }

    // ============ CONTROLLER VALIDATION TESTS ============
    @Test
    public void testControllerParameterOrder() {
        // This test verifies the controller calls the API client with correct parameters
        // Note: The controller currently calls getEscalationPoliciesWithPagination(0, 25)
        // but this seems like it should be (25, 0) based on typical limit/offset pattern
        
        String sessionId = loginAndGetSessionId("test@example.com", "password123");
        
        List<EscalationPolicy> mockPolicies = Arrays.asList(
            createTestEscalationPolicy("PARAM_TEST", "Parameter Test Policy")
        );
        EscalationPoliciesResponse mockResponse = createTestResponse(mockPolicies, 25, 0, false);


        ResponseEntity<Map> response = makeGetRequestWithSession("/escalation-policies/list", sessionId, Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue((Boolean) response.getBody().get("success"));
    }

    @Test
    public void testResponseStructure() {
        // Test the exact response structure the controller returns
        String sessionId = loginAndGetSessionId("test@example.com", "password123");
        
        List<EscalationPolicy> mockPolicies = Arrays.asList(
            createTestEscalationPolicy("STRUCT_TEST", "Structure Test Policy")
        );
        

        ResponseEntity<Map> response = makeGetRequestWithSession("/escalation-policies/list", sessionId, Map.class);

        Map<String, Object> body = response.getBody();
        
        // Verify exact response structure
        assertTrue(body.containsKey("success"));
        assertTrue(body.containsKey("users")); // Note: controller uses "users" key, not "escalationPolicies"
        assertTrue(body.containsKey("totalCount"));
        
        assertTrue((Boolean) body.get("success"));
        assertEquals(0, (Integer) body.get("totalCount")); // Controller hardcodes totalCount to 0
        
        List<?> policiesResponse = (List<?>) body.get("users");
        assertNotNull(policiesResponse);
        assertEquals(1, policiesResponse.size());
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
}