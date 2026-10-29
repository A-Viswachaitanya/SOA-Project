package com.recurringflex.subscriptionservice.dto;

import jakarta.validation.constraints.NotNull;

public class SubscribeRequest {
    // Optional: defaults to the logged-in user. Only an admin may subscribe someone else.
    private Long userId;

    @NotNull(message = "planId is required")
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
