package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.model.Membership;
import com.example.demo.model.MembershipPlan;
import com.example.demo.model.Payment;
import com.example.demo.model.User;
import com.example.demo.model.enums.MembershipStatus;
import com.example.demo.model.enums.PaymentMethod;
import com.example.demo.model.enums.PaymentStatus;
import com.example.demo.model.enums.Role;
import com.example.demo.repository.MembershipRepository;
import com.example.demo.repository.PaymentRepository;
import com.example.demo.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MembershipService {

    private final MembershipRepository membershipRepository;
    private final PaymentRepository paymentRepository;
    private final MembershipPlanService planService;
    private final UserService userService;
    private final Clock clock;

    /** Zakup karnetu przez zalogowanego klubowicza (płatność od razu zaksięgowana). */
    @Transactional
    public MembershipResponse purchase(Long userId, PurchaseRequest req) {
        return DtoMapper.toResponse(createMembership(userService.getEntity(userId), req.planId(),
                req.paymentMethod(), req.startDate()));
    }

    /** Sprzedaż karnetu na recepcji. */
    @Transactional
    public MembershipResponse sell(SellMembershipRequest req) {
        return DtoMapper.toResponse(createMembership(userService.getEntity(req.userId()), req.planId(),
                req.paymentMethod(), req.startDate()));
    }

    @Transactional(readOnly = true)
    public List<MembershipResponse> findForUser(Long userId) {
        return membershipRepository.findAllByMemberIdOrderByStartDateDesc(userId).stream()
                .map(DtoMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MembershipResponse> findAll() {
        return membershipRepository.findAllByOrderByStartDateDesc().stream()
                .map(DtoMapper::toResponse).toList();
    }

    /** Anulowanie karnetu (administrator lub właściciel). Zaksięgowana płatność zostaje oznaczona jako zwrócona. */
    @Transactional
    public MembershipResponse cancel(Long membershipId, AuthUser currentUser) {
        Membership m = getEntity(membershipId);
        if (!currentUser.isAdmin() && !m.getMember().getId().equals(currentUser.id())) {
            throw new AccessDeniedException("Nie możesz anulować cudzego karnetu");
        }
        if (m.getStatus() != MembershipStatus.ACTIVE) {
            throw new BusinessException("Można anulować tylko aktywny karnet");
        }
        m.setStatus(MembershipStatus.CANCELLED);
        paymentRepository.findAllByMembershipId(membershipId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.COMPLETED)
                .forEach(p -> p.setStatus(PaymentStatus.REFUNDED));
        return DtoMapper.toResponse(m);
    }

    /**
     * Karnet uprawniający do wejścia dziś. Przy kilku ważnych wybiera ten, który kończy się najwcześniej.
     */
    @Transactional(readOnly = true)
    public Optional<Membership> findValidMembership(Long userId, boolean requireClasses) {
        LocalDate today = LocalDate.now(clock);
        return membershipRepository.findAllByMemberIdAndStatus(userId, MembershipStatus.ACTIVE).stream()
                .filter(m -> m.isValidOn(today))
                .filter(m -> !requireClasses || m.getPlan().isIncludesClasses())
                .min(Comparator.comparing(Membership::getEndDate));
    }

    /** Wywoływane codziennie przez harmonogram – oznacza przeterminowane karnety jako wygasłe. */
    @Transactional
    public int expireOutdatedMemberships() {
        return membershipRepository.expireOutdated(LocalDate.now(clock), MembershipStatus.ACTIVE, MembershipStatus.EXPIRED);
    }

    Membership getEntity(Long id) {
        return membershipRepository.findById(id).orElseThrow(() -> NotFoundException.of("Karnet klubowicza", id));
    }

    private Membership createMembership(User member, Long planId, PaymentMethod method, LocalDate startDate) {
        if (member.getRole() != Role.MEMBER) {
            throw new BusinessException("Karnet można sprzedać tylko klubowiczowi");
        }
        if (!member.isActive()) {
            throw new BusinessException("Konto klubowicza jest zablokowane");
        }
        MembershipPlan plan = planService.getEntity(planId);
        if (!plan.isActive()) {
            throw new BusinessException("Ten karnet nie jest już w sprzedaży");
        }
        LocalDate start = startDate != null ? startDate : LocalDate.now(clock);
        Membership membership = Membership.builder()
                .member(member)
                .plan(plan)
                .startDate(start)
                .endDate(start.plusDays(plan.getDurationDays() - 1L))
                .remainingEntries(plan.getEntryLimit())
                .status(MembershipStatus.ACTIVE)
                .build();
        membership = membershipRepository.save(membership);

        Payment payment = Payment.builder()
                .membership(membership)
                .amount(plan.getPrice())
                .method(method)
                .status(PaymentStatus.COMPLETED)
                .paidAt(LocalDateTime.now(clock))
                .build();
        paymentRepository.save(payment);
        return membership;
    }
}
