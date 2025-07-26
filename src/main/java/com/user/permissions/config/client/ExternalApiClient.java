package com.user.permissions.config.client;


import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.user.permissions.escalationpolicies.model.EscalationPoliciesResponse;
import com.user.permissions.escalationpolicies.model.EscalationPolicy;

@Component
public class ExternalApiClient {

    private final RestTemplate restTemplate;
    private static final Integer DEFAULT_LIMIT = 25;
    private static final Integer DEFAULT_OFFSET = 0;

    
    @Value("${pagerduty.api.baseUrl}")
    private String baseUrl;
    
    @Value("${pagerduty.api.token}")
    private String apiToken;

    public ExternalApiClient() {
        this.restTemplate = new RestTemplate();
    }

    public List<EscalationPolicy> getEscalationPolicies(Integer limit, Integer offset) {
        try {
           
        	 // Set default values if null
            if (limit == null) limit = DEFAULT_LIMIT;
            if (offset == null) offset = DEFAULT_OFFSET;

            String url = String.format("%s/escalation_policies?limit=%d&offset=%d", 
                                     baseUrl, limit, offset);

            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<EscalationPoliciesResponse> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity, 
                EscalationPoliciesResponse.class
            );

            if (response.getBody() != null && response.getBody().getEscalationPolicies() != null) {
                return response.getBody().getEscalationPolicies();
            }

            return Collections.emptyList();            
        } catch (HttpClientErrorException e) {
            System.err.println("Client error calling PagerDuty API: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return Collections.emptyList();
        } catch (HttpServerErrorException e) {
            System.err.println("Server error calling PagerDuty API: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return Collections.emptyList();
        } catch (Exception e) {
            System.err.println("Unexpected error calling PagerDuty API: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    public Optional<EscalationPoliciesResponse> getEscalationPoliciesWithPagination(Integer limit, Integer offset, Boolean includeTotal, String query) {
        try {
            if (limit == null) limit = DEFAULT_LIMIT;
            if (offset == null) offset = DEFAULT_OFFSET;
            if (includeTotal == null) includeTotal = false;
            

            String url = String.format("%s/escalation_policies?limit=%d&offset=%d&total=%s%s", 
                                     baseUrl, limit, offset, includeTotal.toString(), 
                                     !query.equals("")? "&query="+query:"");
            	

            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<EscalationPoliciesResponse> response = restTemplate.exchange(
                url, 
                HttpMethod.GET, 
                entity, 
                EscalationPoliciesResponse.class
            );
            

            return Optional.ofNullable(response.getBody());

        } catch (HttpClientErrorException e) {
            System.err.println("Client error calling PagerDuty API: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return Optional.empty();
        } catch (HttpServerErrorException e) {
            System.err.println("Server error calling PagerDuty API: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return Optional.empty();
        } catch (Exception e) {
            System.err.println("Unexpected error calling PagerDuty API: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    
        
 // Keep the existing method for backward compatibility
    public Optional<EscalationPoliciesResponse> getEscalationPoliciesWithPagination(Integer limit, Integer offset) {
        return getEscalationPoliciesWithPagination(limit, offset, false,"");
    }
    
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");
        headers.set("Authorization", "Token token=" + apiToken);
        return headers;
    }
}