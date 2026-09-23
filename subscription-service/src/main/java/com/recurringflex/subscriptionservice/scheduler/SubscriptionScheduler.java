package com.recurringflex.subscriptionservice.scheduler;

import com.recurringflex.subscriptionservice.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionScheduler {

    @Autowired
    private SubscriptionService subscriptionService;

    // Run every day at midnight to check for expired subscriptions
    @Scheduled(cron = "0 0 0 * * *")
    public void checkExpiredSubscriptions() {
        System.out.println("Running scheduled task: Checking for expired subscriptions...");
        subscriptionService.expireSubscriptions();
    }

    // Run every day at 1 AM to auto-renew eligible subscriptions
    @Scheduled(cron = "0 0 1 * * *")
    public void autoRenewSubscriptions() {
        System.out.println("Running scheduled task: Auto-renewing eligible subscriptions...");
        subscriptionService.autoRenewSubscriptions();
    }
}
