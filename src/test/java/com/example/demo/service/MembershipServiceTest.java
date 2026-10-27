package com.example.demo.service;

import com.example.demo.dto.MembershipResponse;
import com.example.demo.dto.PurchaseRequest;
import com.example.demo.exception.BusinessException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembershipServiceTest {

    @Mock MembershipRepository membershipRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock MembershipPlanService planService;
    @Mock UserService userService;

    MembershipService service;
    User member = TestData.user(1, Role.MEMBER);

    @BeforeEach
    void setUp() {
        service = new MembershipService(membershipRepository, paymentRepository, planService, userService, TestData.CLOCK);
    }

    @Test
    void purchaseCreatesMembershipAndCompletedPayment() {
        MembershipPlan plan = TestData.plan(3, 30, 10, true);
        when(userService.getEntity(1L)).thenReturn(member);
        when(planService.getEntity(3L)).thenReturn(plan);
        when(membershipRepository.save(any(Membership.class))).thenAnswer(inv -> inv.getArgument(0));

        MembershipResponse result = service.purchase(1L, new PurchaseRequest(3L, PaymentMethod.CARD, null));

        LocalDate today = TestData.NOW.toLocalDate();
        assertThat(result.startDate()).isEqualTo(today);
        assertThat(result.endDate()).isEqualTo(today.plusDays(29)); // 30 dni łącznie z dniem startu
        assertThat(result.remainingEntries()).isEqualTo(10);
        assertThat(result.status()).isEqualTo(MembershipStatus.ACTIVE);

        ArgumentCaptor<Payment> payment = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(payment.capture());
        assertThat(payment.getValue().getAmount()).isEqualByComparingTo("100.00");
        assertThat(payment.getValue().getStatus()).isEqualTo(PaymentStatus.COMPLETED);
    }

    @Test
    void cannotBuyPlanThatIsNoLongerSold() {
        MembershipPlan plan = TestData.plan(3, 30, null, true);
        plan.setActive(false);
        when(userService.getEntity(1L)).thenReturn(member);
        when(planService.getEntity(3L)).thenReturn(plan);

        assertThatThrownBy(() -> service.purchase(1L, new PurchaseRequest(3L, PaymentMethod.CASH, null)))
                .isInstanceOf(BusinessException.class);
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void trainerCannotBuyMembership() {
        when(userService.getEntity(2L)).thenReturn(TestData.user(2, Role.TRAINER));

        assertThatThrownBy(() -> service.purchase(2L, new PurchaseRequest(3L, PaymentMethod.CASH, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("klubowiczowi");
    }

    @Test
    void cancelMarksMembershipCancelledAndRefundsPayment() {
        Membership m = TestData.membership(5, member, TestData.plan(3, 30, null, true), null);
        Payment payment = Payment.builder().id(9L).membership(m).status(PaymentStatus.COMPLETED).build();
        when(membershipRepository.findById(5L)).thenReturn(Optional.of(m));
        when(paymentRepository.findAllByMembershipId(5L)).thenReturn(List.of(payment));

        service.cancel(5L, new AuthUser(1L, member.getEmail(), Role.MEMBER));

        assertThat(m.getStatus()).isEqualTo(MembershipStatus.CANCELLED);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void memberCannotCancelSomeoneElsesMembership() {
        Membership m = TestData.membership(5, member, TestData.plan(3, 30, null, true), null);
        when(membershipRepository.findById(5L)).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> service.cancel(5L, new AuthUser(99L, "inny@gym.pl", Role.MEMBER)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void findValidMembershipSkipsPlansWithoutClassesWhenRequired() {
        Membership noClasses = TestData.membership(5, member, TestData.plan(3, 30, null, false), null);
        when(membershipRepository.findAllByMemberIdAndStatus(1L, MembershipStatus.ACTIVE)).thenReturn(List.of(noClasses));

        assertThat(service.findValidMembership(1L, true)).isEmpty();
        assertThat(service.findValidMembership(1L, false)).contains(noClasses);
    }
}
