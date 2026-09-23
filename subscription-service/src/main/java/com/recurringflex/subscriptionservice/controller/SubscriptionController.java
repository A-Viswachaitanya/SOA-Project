package com.recurringflex.subscriptionservice.controller;

import com.recurringflex.subscriptionservice.dto.SubscribeRequest;
import com.recurringflex.subscriptionservice.entity.Subscription;
import com.recurringflex.subscriptionservice.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(@RequestBody SubscribeRequest request) {
        try {
            Subscription subscription = subscriptionService.subscribe(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(subscription);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getSubscriptionById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(subscriptionService.getSubscriptionById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Subscription>> getSubscriptionsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(subscriptionService.getSubscriptionsByUserId(userId));
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<Subscription>> getActiveSubscriptionsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(subscriptionService.getActiveSubscriptionsByUserId(userId));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelSubscription(@PathVariable Long id) {
        try {
            Subscription subscription = subscriptionService.cancelSubscription(id);
            return ResponseEntity.ok(subscription);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/renew")
    public ResponseEntity<?> renewSubscription(@PathVariable Long id) {
        try {
            Subscription subscription = subscriptionService.renewSubscription(id);
            return ResponseEntity.ok(subscription);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Subscription>> getAllSubscriptions() {
        return ResponseEntity.ok(subscriptionService.getAllSubscriptions());
    }
}
