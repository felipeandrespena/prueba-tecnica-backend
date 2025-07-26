package com.user.permissions.escalationpolicies.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EscalationPolicy {
    private String id;
    private String type;
    private String summary;
    
    @JsonProperty("on_call_handoff_notifications")
    private String onCallHandoffNotifications;
    
    private String self;
    
    @JsonProperty("html_url")
    private String htmlUrl;
    
    private String name;
    
    @JsonProperty("num_loops")
    private Integer numLoops;

    // Constructors
    public EscalationPolicy() {}

    public EscalationPolicy(String id, String name, String summary) {
        this.id = id;
        this.name = name;
        this.summary = summary;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getOnCallHandoffNotifications() { return onCallHandoffNotifications; }
    public void setOnCallHandoffNotifications(String onCallHandoffNotifications) { 
        this.onCallHandoffNotifications = onCallHandoffNotifications; 
    }

    public String getSelf() { return self; }
    public void setSelf(String self) { this.self = self; }

    public String getHtmlUrl() { return htmlUrl; }
    public void setHtmlUrl(String htmlUrl) { this.htmlUrl = htmlUrl; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getNumLoops() { return numLoops; }
    public void setNumLoops(Integer numLoops) { this.numLoops = numLoops; }

    @Override
    public String toString() {
        return "EscalationPolicy{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", summary='" + summary + '\'' +
                '}';
    }
}