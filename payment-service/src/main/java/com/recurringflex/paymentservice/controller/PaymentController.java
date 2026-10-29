package com.recurringflex.paymentservice.controller;

import com.recurringflex.paymentservice.dto.PaymentRequest;
import com.recurringflex.paymentservice.entity.Payment;
import com.recurringflex.paymentservice.security.CurrentUser;
import com.recurringflex.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Errors are mapped to HTTP status codes by GlobalExceptionHandler.
// Users can only read their own payments; creating, refunding and deleting is ADMIN/SYSTEM only.
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    // Called by subscription-service (SYSTEM role) when a subscription is created or renewed
    @PostMapping("/process")
    public ResponseEntity<Payment> processPayment(@Valid @RequestBody PaymentRequest request) {
        CurrentUser.requireAdmin();
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.processPayment(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable Long id) {
        Payment payment = paymentService.getPaymentById(id);
        CurrentUser.requireSelfOrAdmin(payment.getUserId());
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/subscription/{subscriptionId}")
    public ResponseEntity<List<Payment>> getPaymentsBySubscriptionId(@PathVariable Long subscriptionId) {
        List<Payment> payments = paymentService.getPaymentsBySubscriptionId(subscriptionId);
        if (!CurrentUser.isAdmin()) {
            Long callerId = CurrentUser.id();
            payments = payments.stream().filter(p -> p.getUserId().equals(callerId)).toList();
        }
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Payment>> getPaymentsByUserId(@PathVariable Long userId) {
        CurrentUser.requireSelfOrAdmin(userId);
        return ResponseEntity.ok(paymentService.getPaymentsByUserId(userId));
    }

    @GetMapping
    public ResponseEntity<List<Payment>> getAllPayments() {
        CurrentUser.requireAdmin();
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @PutMapping("/{id}/refund")
    public ResponseEntity<Payment> refundPayment(@PathVariable Long id) {
        CurrentUser.requireAdmin();
        return ResponseEntity.ok(paymentService.refundPayment(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deletePayment(@PathVariable Long id) {
        CurrentUser.requireAdmin();
        paymentService.deletePayment(id);
        return ResponseEntity.ok(Map.of("message", "Payment deleted successfully"));
    }
}
