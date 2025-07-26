package com.user.permissions.escalationpolicies.request;
import java.util.List;

public class EscalationPolicyRequest {
        private Integer page = 1;
        private Integer size = 10;
        private Boolean includeTotal = false;
        private String query = "";
        private List<String> teamId; // Note: Spring will map team_id[] to this
        
        // Getters and setters
        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }
        
        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }
        
        public Boolean getIncludeTotal() { return includeTotal; }
        public void setIncludeTotal(Boolean includeTotal) { this.includeTotal = includeTotal; }
        
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        
        public List<String> getTeamId() { return teamId; }
        public void setTeamId(List<String> teamId) { this.teamId = teamId; }
    }