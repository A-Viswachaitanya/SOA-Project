package com.recurringflex.subscriptionservice.service;

import com.recurringflex.subscriptionservice.entity.Plan;
import com.recurringflex.subscriptionservice.repository.PlanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlanService {

    @Autowired
    private PlanRepository planRepository;

    public Plan createPlan(Plan plan) {
        return planRepository.save(plan);
    }

    public List<Plan> getAllPlans() {
        return planRepository.findAll();
    }

    public List<Plan> getActivePlans() {
        return planRepository.findByActiveTrue();
    }

    public Plan getPlanById(Long id) {
        return planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan not found with id: " + id));
    }

    public Plan updatePlan(Long id, Plan planDetails) {
        Plan plan = getPlanById(id);
        plan.setName(planDetails.getName());
        plan.setDescription(planDetails.getDescription());
        plan.setPrice(planDetails.getPrice());
        plan.setDurationInDays(planDetails.getDurationInDays());
        plan.setActive(planDetails.getActive());
        return planRepository.save(plan);
    }

    public void deletePlan(Long id) {
        Plan plan = getPlanById(id);
        plan.setActive(false);
        planRepository.save(plan);
    }
}
