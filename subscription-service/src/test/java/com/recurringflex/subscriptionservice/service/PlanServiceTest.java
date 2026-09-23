package com.recurringflex.subscriptionservice.service;

import com.recurringflex.subscriptionservice.entity.Plan;
import com.recurringflex.subscriptionservice.repository.PlanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PlanServiceTest {

    @Mock
    private PlanRepository planRepository;

    @InjectMocks
    private PlanService planService;

    private Plan basicPlan;

    @BeforeEach
    void setUp() {
        basicPlan = new Plan();
        basicPlan.setId(1L);
        basicPlan.setName("Basic Tier");
        basicPlan.setDescription("Standard features");
        basicPlan.setPrice(BigDecimal.valueOf(9.99));
        basicPlan.setDurationInDays(30);
        basicPlan.setActive(true);
    }

    @Test
    @DisplayName("Create Plan: successfully saves and returns the plan")
    void testCreatePlan() {
        when(planRepository.save(any(Plan.class))).thenReturn(basicPlan);

        Plan saved = planService.createPlan(basicPlan);

        assertNotNull(saved);
        assertEquals("Basic Tier", saved.getName());
        assertEquals(BigDecimal.valueOf(9.99), saved.getPrice());
        verify(planRepository, times(1)).save(basicPlan);
    }

    @Test
    @DisplayName("Get Plan by ID: returns plan when found")
    void testGetPlanById_Success() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(basicPlan));

        Plan found = planService.getPlanById(1L);

        assertNotNull(found);
        assertEquals(1L, found.getId());
        assertEquals("Basic Tier", found.getName());
    }

    @Test
    @DisplayName("Get Plan by ID: throws RuntimeException when not found")
    void testGetPlanById_NotFound_ThrowsException() {
        when(planRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> planService.getPlanById(99L));
    }

    @Test
    @DisplayName("Update Plan: updates plan details and returns saved plan")
    void testUpdatePlan() {
        Plan updateDetails = new Plan();
        updateDetails.setName("Premium Tier");
        updateDetails.setDescription("All features unlocked");
        updateDetails.setPrice(BigDecimal.valueOf(29.99));
        updateDetails.setDurationInDays(60);
        updateDetails.setActive(true);

        when(planRepository.findById(1L)).thenReturn(Optional.of(basicPlan));
        when(planRepository.save(any(Plan.class))).thenAnswer(i -> i.getArgument(0));

        Plan updated = planService.updatePlan(1L, updateDetails);

        assertEquals("Premium Tier", updated.getName());
        assertEquals(BigDecimal.valueOf(29.99), updated.getPrice());
        assertEquals(60, updated.getDurationInDays());
    }

    @Test
    @DisplayName("Delete Plan: soft deletes plan by setting active to false")
    void testDeletePlan() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(basicPlan));
        when(planRepository.save(any(Plan.class))).thenAnswer(i -> i.getArgument(0));

        planService.deletePlan(1L);

        assertFalse(basicPlan.getActive());
        verify(planRepository, times(1)).save(basicPlan);
    }

    @Test
    @DisplayName("Get Active Plans: returns only active plans")
    void testGetActivePlans() {
        when(planRepository.findByActiveTrue()).thenReturn(List.of(basicPlan));

        List<Plan> activePlans = planService.getActivePlans();

        assertEquals(1, activePlans.size());
        assertTrue(activePlans.get(0).getActive());
    }
}
