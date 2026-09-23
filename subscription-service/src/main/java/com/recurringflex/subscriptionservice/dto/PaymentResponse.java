package com.recurringflex.subscriptionservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentResponse {
    private Long id;
    private Long subscriptionId;
    private Long userId;
    private BigDecimal amount;
    private String status;
    private String transactionRef;
    private LocalDateTime paymentDate;

    public PaymentResponse() {}
    public PaymentResponse(Long id, Long subscriptionId, Long userId, BigDecimal amount, String status, String transactionRef, LocalDateTime paymentDate) {
        this.id = id; this.subscriptionId = subscriptionId; this.userId = userId; this.amount = amount;
        this.status = status; this.transactionRef = transactionRef; this.paymentDate = paymentDate;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(Long subscriptionId) { this.subscriptionId = subscriptionId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
}
