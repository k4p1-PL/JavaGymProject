package com.example.demo.service;

import com.example.demo.dto.BookingResponse;
import com.example.demo.dto.DtoMapper;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.model.Booking;
import com.example.demo.model.GymClass;
import com.example.demo.model.User;
import com.example.demo.model.enums.BookingStatus;
import com.example.demo.model.enums.ClassStatus;
import com.example.demo.model.enums.Role;
import com.example.demo.repository.BookingRepository;
import com.example.demo.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookingService {

    /** Rezerwację można anulować najpóźniej tyle godzin przed zajęciami. */
    static final int CANCEL_DEADLINE_HOURS = 2;

    private final BookingRepository bookingRepository;
    private final GymClassService gymClassService;
    private final MembershipService membershipService;
    private final UserService userService;
    private final Clock clock;

    @Transactional
    public BookingResponse book(Long userId, Long gymClassId) {
        User member = userService.getEntity(userId);
        if (member.getRole() != Role.MEMBER) {
            throw new BusinessException("Na zajęcia mogą zapisywać się tylko klubowicze");
        }
        GymClass gymClass = gymClassService.getEntity(gymClassId);
        LocalDateTime now = LocalDateTime.now(clock);

        if (gymClass.getStatus() != ClassStatus.SCHEDULED) {
            throw new BusinessException("Zajęcia zostały odwołane lub już się odbyły");
        }
        if (!gymClass.getStartTime().isAfter(now)) {
            throw new BusinessException("Nie można zapisać się na zajęcia, które już się rozpoczęły");
        }
        if (bookingRepository.existsByMemberIdAndGymClassIdAndStatusIn(userId, gymClassId, Set.of(BookingStatus.BOOKED))) {
            throw new BusinessException("Jesteś już zapisany na te zajęcia");
        }
        if (membershipService.findValidMembership(userId, true).isEmpty()) {
            throw new BusinessException("Brak ważnego karnetu obejmującego zajęcia grupowe");
        }
        if (gymClassService.countBooked(gymClassId) >= gymClass.getCapacity()) {
            throw new BusinessException("Brak wolnych miejsc na tych zajęciach");
        }

        Booking booking = Booking.builder()
                .member(member)
                .gymClass(gymClass)
                .bookedAt(now)
                .status(BookingStatus.BOOKED)
                .build();
        return DtoMapper.toResponse(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> findForUser(Long userId) {
        return bookingRepository.findAllByMemberIdOrderByBookedAtDesc(userId).stream()
                .map(DtoMapper::toResponse).toList();
    }

    @Transactional
    public BookingResponse cancel(Long bookingId, AuthUser currentUser) {
        Booking booking = getEntity(bookingId);
        boolean isOwner = booking.getMember().getId().equals(currentUser.id());
        if (!isOwner && !currentUser.isAdmin()) {
            throw new AccessDeniedException("Nie możesz anulować cudzej rezerwacji");
        }
        if (booking.getStatus() != BookingStatus.BOOKED) {
            throw new BusinessException("Ta rezerwacja nie jest aktywna");
        }
        LocalDateTime deadline = booking.getGymClass().getStartTime().minusHours(CANCEL_DEADLINE_HOURS);
        if (!currentUser.isAdmin() && LocalDateTime.now(clock).isAfter(deadline)) {
            throw new BusinessException("Rezerwację można anulować najpóźniej " + CANCEL_DEADLINE_HOURS + " godz. przed zajęciami");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        return DtoMapper.toResponse(booking);
    }

    /** Trener (lub administrator) zaznacza obecność klubowicza na zajęciach. */
    @Transactional
    public BookingResponse markAttendance(Long bookingId, boolean attended, AuthUser currentUser) {
        Booking booking = getEntity(bookingId);
        gymClassService.checkCanManage(booking.getGymClass(), currentUser);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException("Rezerwacja została anulowana");
        }
        booking.setStatus(attended ? BookingStatus.ATTENDED : BookingStatus.NO_SHOW);
        return DtoMapper.toResponse(booking);
    }

    private Booking getEntity(Long id) {
        return bookingRepository.findById(id).orElseThrow(() -> NotFoundException.of("Rezerwacja", id));
    }
}
