package com.example.demo.service;

import com.example.demo.dto.CheckInRequest;
import com.example.demo.dto.CheckInResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.model.CheckIn;
import com.example.demo.model.Membership;
import com.example.demo.model.User;
import com.example.demo.model.enums.Role;
import com.example.demo.repository.CheckInRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckInServiceTest {

    @Mock CheckInRepository checkInRepository;
    @Mock UserRepository userRepository;
    @Mock MembershipService membershipService;

    CheckInService service;
    User member = TestData.user(1, Role.MEMBER);

    @BeforeEach
    void setUp() {
        service = new CheckInService(checkInRepository, userRepository, membershipService, TestData.CLOCK);
    }

    @Test
    void checkInUsesOneEntryFromLimitedMembership() {
        Membership membership = TestData.membership(5, member, TestData.plan(3, 60, 10, true), 3);
        when(userRepository.findByEmailIgnoreCase("user1@gym.pl")).thenReturn(Optional.of(member));
        when(checkInRepository.findFirstByMemberIdAndCheckOutTimeIsNull(1L)).thenReturn(Optional.empty());
        when(membershipService.findValidMembership(1L, false)).thenReturn(Optional.of(membership));
        when(checkInRepository.save(any(CheckIn.class))).thenAnswer(inv -> inv.getArgument(0));

        CheckInResponse result = service.checkIn(new CheckInRequest(null, "user1@gym.pl"));

        assertThat(membership.getRemainingEntries()).isEqualTo(2);
        assertThat(result.remainingEntries()).isEqualTo(2);
        assertThat(result.checkInTime()).isEqualTo(TestData.NOW);
    }

    @Test
    void checkInWithoutValidMembershipIsRejected() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(member));
        when(checkInRepository.findFirstByMemberIdAndCheckOutTimeIsNull(1L)).thenReturn(Optional.empty());
        when(membershipService.findValidMembership(1L, false)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.checkIn(new CheckInRequest(1L, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("nie ma ważnego karnetu");
    }

    @Test
    void cannotCheckInTwiceWithoutCheckOut() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(member));
        when(checkInRepository.findFirstByMemberIdAndCheckOutTimeIsNull(1L))
                .thenReturn(Optional.of(CheckIn.builder().id(4L).member(member).build()));

        assertThatThrownBy(() -> service.checkIn(new CheckInRequest(1L, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("jest już na siłowni");
    }

    @Test
    void requiresUserIdOrEmail() {
        assertThatThrownBy(() -> service.checkIn(new CheckInRequest(null, " ")))
                .isInstanceOf(BusinessException.class);
    }
}
