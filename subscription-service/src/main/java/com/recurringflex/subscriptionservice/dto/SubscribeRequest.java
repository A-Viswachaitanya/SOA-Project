package com.recurringflex.subscriptionservice.dto;

public class SubscribeRequest {
    private Long userId;
    private Long planId;
    private Boolean autoRenew = false;

    public SubscribeRequest() {}
    public SubscribeRequest(Long userId, Long planId, Boolean autoRenew) {
        this.userId = userId; this.planId = planId; this.autoRenew = autoRenew;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public Boolean getAutoRenew() { return autoRenew; }
    public void setAutoRenew(Boolean autoRenew) { this.autoRenew = autoRenew; }
}
