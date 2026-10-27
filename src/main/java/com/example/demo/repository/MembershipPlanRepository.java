package com.example.demo.repository;

import com.example.demo.model.MembershipPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, Long> {

    List<MembershipPlan> findAllByOrderByPriceAsc();

    List<MembershipPlan> findAllByActiveTrueOrderByPriceAsc();

    Optional<MembershipPlan> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
