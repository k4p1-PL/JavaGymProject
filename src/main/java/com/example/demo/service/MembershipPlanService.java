package com.example.demo.service;

import com.example.demo.dto.DtoMapper;
import com.example.demo.dto.PlanRequest;
import com.example.demo.dto.PlanResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.model.MembershipPlan;
import com.example.demo.repository.MembershipPlanRepository;
import com.example.demo.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MembershipPlanService {

    private final MembershipPlanRepository planRepository;
    private final MembershipRepository membershipRepository;

    @Transactional(readOnly = true)
    public List<PlanResponse> findAll(boolean onlyActive) {
        var plans = onlyActive ? planRepository.findAllByActiveTrueOrderByPriceAsc() : planRepository.findAllByOrderByPriceAsc();
        return plans.stream().map(DtoMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PlanResponse findById(Long id) {
        return DtoMapper.toResponse(getEntity(id));
    }

    @Transactional
    public PlanResponse create(PlanRequest req) {
        if (planRepository.existsByNameIgnoreCase(req.name().trim())) {
            throw new BusinessException("Karnet o nazwie \"" + req.name() + "\" już istnieje");
        }
        MembershipPlan plan = new MembershipPlan();
        apply(plan, req);
        return DtoMapper.toResponse(planRepository.save(plan));
    }

    @Transactional
    public PlanResponse update(Long id, PlanRequest req) {
        MembershipPlan plan = getEntity(id);
        planRepository.findByNameIgnoreCase(req.name().trim())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new BusinessException("Karnet o nazwie \"" + req.name() + "\" już istnieje");
                });
        apply(plan, req);
        return DtoMapper.toResponse(plan);
    }

    /** Plan, który ktoś już kupił, nie może zniknąć z historii – wtedy wycofujemy go ze sprzedaży. */
    @Transactional
    public void delete(Long id) {
        MembershipPlan plan = getEntity(id);
        if (membershipRepository.existsByPlanId(id)) {
            throw new BusinessException("Ten karnet został już sprzedany – zamiast usuwać, oznacz go jako nieaktywny");
        }
        planRepository.delete(plan);
    }

    MembershipPlan getEntity(Long id) {
        return planRepository.findById(id).orElseThrow(() -> NotFoundException.of("Karnet", id));
    }

    private void apply(MembershipPlan plan, PlanRequest req) {
        plan.setName(req.name().trim());
        plan.setDescription(req.description());
        plan.setDurationDays(req.durationDays());
        plan.setEntryLimit(req.entryLimit());
        plan.setPrice(req.price());
        plan.setIncludesClasses(req.includesClasses());
        plan.setActive(req.active());
    }
}
