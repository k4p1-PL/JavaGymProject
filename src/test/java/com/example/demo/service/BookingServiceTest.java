package com.example.demo.service;

import com.example.demo.dto.BookingResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.model.Booking;
import com.example.demo.model.GymClass;
import com.example.demo.model.User;
import com.example.demo.model.enums.BookingStatus;
import com.example.demo.model.enums.Role;
import com.example.demo.repository.BookingRepository;
import com.example.demo.security.AuthUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock GymClassService gymClassService;
    @Mock MembershipService membershipService;
    @Mock UserService userService;

    BookingService service;

    User member = TestData.user(1, Role.MEMBER);
    User trainer = TestData.user(2, Role.TRAINER);
    GymClass gymClass = TestData.gymClass(10, trainer, TestData.NOW.plusDays(1), 2);

    @BeforeEach
    void setUp() {
        service = new BookingService(bookingRepository, gymClassService, membershipService, userService, TestData.CLOCK);
    }

    private void givenMemberAndClass() {
        when(userService.getEntity(1L)).thenReturn(member);
        when(gymClassService.getEntity(10L)).thenReturn(gymClass);
    }

    @Test
    void booksClassWhenMembershipValidAndSpotsAvailable() {
        givenMemberAndClass();
        when(membershipService.findValidMembership(1L, true))
                .thenReturn(Optional.of(TestData.membership(5, member, TestData.plan(3, 30, null, true), null)));
        when(gymClassService.countBooked(10L)).thenReturn(1L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse result = service.book(1L, 10L);

        assertThat(result.status()).isEqualTo(BookingStatus.BOOKED);
        assertThat(result.gymClassId()).isEqualTo(10L);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    void rejectsBookingWhenClassIsFull() {
        givenMemberAndClass();
        when(membershipService.findValidMembership(1L, true))
                .thenReturn(Optional.of(TestData.membership(5, member, TestData.plan(3, 30, null, true), null)));
        when(gymClassService.countBooked(10L)).thenReturn(2L);

        assertThatThrownBy(() -> service.book(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Brak wolnych miejsc");
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void rejectsBookingWithoutValidMembership() {
        givenMemberAndClass();
        when(membershipService.findValidMembership(1L, true)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.book(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("karnetu");
    }

    @Test
    void rejectsBookingOfClassThatAlreadyStarted() {
        gymClass.setStartTime(TestData.NOW.minusMinutes(5));
        givenMemberAndClass();

        assertThatThrownBy(() -> service.book(1L, 10L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("rozpoczęły");
    }

    @Test
    void memberCannotCancelLessThanTwoHoursBeforeClass() {
        gymClass.setStartTime(TestData.NOW.plusHours(1));
        Booking booking = Booking.builder().id(7L).member(member).gymClass(gymClass)
                .bookedAt(TestData.NOW.minusDays(1)).status(BookingStatus.BOOKED).build();
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));

        AuthUser me = new AuthUser(1L, member.getEmail(), Role.MEMBER);

        assertThatThrownBy(() -> service.cancel(7L, me))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("najpóźniej");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.BOOKED);
    }

    @Test
    void memberCanCancelOwnBookingInTime() {
        Booking booking = Booking.builder().id(7L).member(member).gymClass(gymClass)
                .bookedAt(TestData.NOW.minusDays(1)).status(BookingStatus.BOOKED).build();
        when(bookingRepository.findById(7L)).thenReturn(Optional.of(booking));

        BookingResponse result = service.cancel(7L, new AuthUser(1L, member.getEmail(), Role.MEMBER));

        assertThat(result.status()).isEqualTo(BookingStatus.CANCELLED);
    }
}
