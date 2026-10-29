package com.recurringflex.subscriptionservice.controller;

import com.recurringflex.subscriptionservice.dto.SubscribeRequest;
import com.recurringflex.subscriptionservice.entity.Subscription;
import com.recurringflex.subscriptionservice.security.CurrentUser;
import com.recurringflex.subscriptionservice.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Errors are mapped to HTTP status codes by GlobalExceptionHandler.
// Users can only see and manage their own subscriptions; admins can manage all.
@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    @PostMapping("/subscribe")
    public ResponseEntity<Subscription> subscribe(@Valid @RequestBody SubscribeRequest request) {
        if (request.getUserId() == null) {
            request.setUserId(CurrentUser.id());
        } else {
            CurrentUser.requireSelfOrAdmin(request.getUserId());
        }
        if (request.getUserId() == null) {
            throw new RuntimeException("userId is required");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(subscriptionService.subscribe(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Subscription> getSubscriptionById(@PathVariable Long id) {
        Subscription subscription = subscriptionService.getSubscriptionById(id);
        CurrentUser.requireSelfOrAdmin(subscription.getUserId());
        return ResponseEntity.ok(subscription);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Subscription>> getSubscriptionsByUserId(@PathVariable Long userId) {
        CurrentUser.requireSelfOrAdmin(userId);
        return ResponseEntity.ok(subscriptionService.getSubscriptionsByUserId(userId));
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<Subscription>> getActiveSubscriptionsByUserId(@PathVariable Long userId) {
        CurrentUser.requireSelfOrAdmin(userId);
        return ResponseEntity.ok(subscriptionService.getActiveSubscriptionsByUserId(userId));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Subscription> cancelSubscription(@PathVariable Long id) {
        CurrentUser.requireSelfOrAdmin(subscriptionService.getSubscriptionById(id).getUserId());
        return ResponseEntity.ok(subscriptionService.cancelSubscription(id));
    }

    @PutMapping("/{id}/renew")
    public ResponseEntity<Subscription> renewSubscription(@PathVariable Long id) {
        CurrentUser.requireSelfOrAdmin(subscriptionService.getSubscriptionById(id).getUserId());
        return ResponseEntity.ok(subscriptionService.renewSubscription(id));
    }

    @GetMapping
    public ResponseEntity<List<Subscription>> getAllSubscriptions() {
        CurrentUser.requireAdmin();
        return ResponseEntity.ok(subscriptionService.getAllSubscriptions());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteSubscription(@PathVariable Long id) {
        CurrentUser.requireAdmin();
        subscriptionService.deleteSubscription(id);
        return ResponseEntity.ok(Map.of("message", "Subscription deleted successfully"));
    }
}
