package com.recurringflex.apigateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/user")
    public ResponseEntity<Map<String, String>> userServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "User Service is currently unavailable",
                        "message", "Please try again later. The service may be down or experiencing high load."
                ));
    }

    @RequestMapping("/subscription")
    public ResponseEntity<Map<String, String>> subscriptionServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Subscription Service is currently unavailable",
                        "message", "Please try again later. The service may be down or experiencing high load."
                ));
    }

    @RequestMapping("/payment")
    public ResponseEntity<Map<String, String>> paymentServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Payment Service is currently unavailable",
                        "message", "Please try again later. The service may be down or experiencing high load."
                ));
    }
}
