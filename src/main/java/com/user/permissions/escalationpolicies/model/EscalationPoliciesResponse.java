package com.user.permissions.escalationpolicies.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EscalationPoliciesResponse {
    @JsonProperty("escalation_policies")
    private List<EscalationPolicy> escalationPolicies;
    
    private Integer limit;
    private Integer offset;
    private Boolean more;
    private Integer total;

    // Constructors
    public EscalationPoliciesResponse() {}

    public EscalationPoliciesResponse(List<EscalationPolicy> escalationPolicies, Integer limit, Integer offset, Boolean more) {
        this.escalationPolicies = escalationPolicies;
        this.limit = limit;
        this.offset = offset;
        this.more = more;
    }

    // Getters and Setters
    public List<EscalationPolicy> getEscalationPolicies() { return escalationPolicies; }
    public void setEscalationPolicies(List<EscalationPolicy> escalationPolicies) { 
        this.escalationPolicies = escalationPolicies; 
    }

    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }

    public Integer getOffset() { return offset; }
    public void setOffset(Integer offset) { this.offset = offset; }

    public Boolean getMore() { return more; }
    public void setMore(Boolean more) { this.more = more; }

    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }

    @Override
    public String toString() {
        return "EscalationPoliciesResponse{" +
                "escalationPolicies=" + (escalationPolicies != null ? escalationPolicies.size() : 0) + " items" +
                ", limit=" + limit +
                ", offset=" + offset +
                ", more=" + more +
                ", total=" + total +
                '}';
    }
}