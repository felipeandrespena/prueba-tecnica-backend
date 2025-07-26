package com.user.permissions.escalationpolicies.controller;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.user.permissions.config.client.ExternalApiClient;
import com.user.permissions.escalationpolicies.model.EscalationPoliciesResponse;
import com.user.permissions.escalationpolicies.request.EscalationPolicyRequest;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/escalation-policies")
public class EscalationPoliciesController {
    
    @Autowired
    private ExternalApiClient externalApiClient;
    
    @GetMapping("/list")
    public ResponseEntity<?> listEscalationPolicies(EscalationPolicyRequest request, HttpSession session) {	
    	
    	int page = request.getPage();
    	int size = request.getSize();
    	String query = request.getQuery();
        
        try {
            // Validate parameters
            if (page < 1) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Page number must be greater than 0"
                ));
            }
            
            if (size < 1 || size > 100) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Page size must be between 1 and 100"
                ));
            }
            
            // Calculate offset (PagerDuty uses 0-based offset)
            int offset = (page - 1) * size;
            
            // Call API with total=true to get total count
            boolean includeTotalInTheResponseFlag = true;
            Optional<EscalationPoliciesResponse> response = externalApiClient.getEscalationPoliciesWithPagination(size, offset, includeTotalInTheResponseFlag, query);
            
            if (response.isPresent()) {
                EscalationPoliciesResponse data = response.get();
                
                // Calculate pagination metadata
                int totalRecords = data.getTotal() != null ? data.getTotal() : 0;
                int totalPages = (int) Math.ceil((double) totalRecords / size);
                boolean hasNext = page < totalPages;
                boolean hasPrevious = page > 1;
                
                return ResponseEntity.ok(Map.of(
                    "success", includeTotalInTheResponseFlag,
                    "escalationPolicies", data.getEscalationPolicies() != null ? data.getEscalationPolicies() : Collections.emptyList(),
                    "pagination", Map.of(
                        "currentPage", page,
                        "pageSize", size,
                        "totalRecords", totalRecords,
                        "totalPages", totalPages,
                        "hasNext", hasNext,
                        "hasPrevious", hasPrevious,
                        "offset", offset,
                        "limit", size
                    )
                ));
            } else {
                // API returned empty - return empty result
                return ResponseEntity.ok(Map.of(
                    "success", includeTotalInTheResponseFlag,
                    "escalationPolicies", Collections.emptyList(),
                    "pagination", Map.of(
                        "currentPage", page,
                        "pageSize", size,
                        "totalRecords", 0,
                        "totalPages", 0,
                        "hasNext", false,
                        "hasPrevious", false,
                        "offset", offset,
                        "limit", size
                    )
                ));
            }
            
        } catch (Exception e) {
            System.err.println("Error retrieving escalation policies: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to retrieve escalation policies. Please try again.",
                "details", e.getMessage()
            ));
        }
    }
}