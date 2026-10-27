package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.model.Booking;
import com.example.demo.model.GymClass;
import com.example.demo.model.User;
import com.example.demo.model.enums.BookingStatus;
import com.example.demo.model.enums.ClassStatus;
import com.example.demo.model.enums.Role;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.GymClassRepository;
import com.example.demo.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class GymClassService {

    /** Statusy rezerwacji, które zajmują miejsce na zajęciach. */
    static final Set<BookingStatus> OCCUPYING = EnumSet.of(BookingStatus.BOOKED, BookingStatus.ATTENDED, BookingStatus.NO_SHOW);

    private final GymClassRepository classRepository;
    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final Clock clock;

    /** Grafik zajęć w przedziale dat (domyślnie od teraz do 14 dni naprzód). */
    @Transactional(readOnly = true)
    public List<GymClassResponse> findSchedule(LocalDateTime from, LocalDateTime to) {
        LocalDateTime start = from != null ? from : LocalDateTime.now(clock);
        LocalDateTime end = to != null ? to : start.plusDays(14);
        if (end.isBefore(start)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Data końcowa jest wcześniejsza niż początkowa");
        }
        return classRepository.findAllByStartTimeBetweenOrderByStartTimeAsc(start, end).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<GymClassResponse> findForTrainer(Long trainerId) {
        return classRepository.findAllByTrainerIdOrderByStartTimeDesc(trainerId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public GymClassResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public GymClassResponse create(GymClassRequest req, AuthUser currentUser) {
        validateTimes(req);
        GymClass gymClass = new GymClass();
        gymClass.setStatus(ClassStatus.SCHEDULED);
        gymClass.setTrainer(resolveTrainer(req, currentUser));
        apply(gymClass, req);
        return toResponse(classRepository.save(gymClass));
    }

    @Transactional
    public GymClassResponse update(Long id, GymClassRequest req, AuthUser currentUser) {
        GymClass gymClass = getEntity(id);
        checkCanManage(gymClass, currentUser);
        validateTimes(req);
        long booked = countBooked(id);
        if (req.capacity() < booked) {
            throw new BusinessException("Na zajęcia zapisało się już " + booked + " osób – limit nie może być mniejszy");
        }
        if (currentUser.isAdmin()) {
            gymClass.setTrainer(resolveTrainer(req, currentUser));
        }
        apply(gymClass, req);
        return toResponse(gymClass);
    }

    /** Odwołanie zajęć – wszystkie aktywne rezerwacje zostają anulowane. */
    @Transactional
    public GymClassResponse cancel(Long id, AuthUser currentUser) {
        GymClass gymClass = getEntity(id);
        checkCanManage(gymClass, currentUser);
        if (gymClass.getStatus() != ClassStatus.SCHEDULED) {
            throw new BusinessException("Można odwołać tylko zaplanowane zajęcia");
        }
        gymClass.setStatus(ClassStatus.CANCELLED);
        bookingRepository.findAllByGymClassIdAndStatus(id, BookingStatus.BOOKED)
                .forEach(b -> b.setStatus(BookingStatus.CANCELLED));
        return toResponse(gymClass);
    }

    /** Usunięcie zajęć możliwe tylko, jeśli nikt się na nie nie zapisał (inaczej – odwołaj). */
    @Transactional
    public void delete(Long id, AuthUser currentUser) {
        GymClass gymClass = getEntity(id);
        checkCanManage(gymClass, currentUser);
        if (bookingRepository.existsByGymClassId(id)) {
            throw new BusinessException("Na zajęcia są zapisy – zamiast usuwać, odwołaj je");
        }
        classRepository.delete(gymClass);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> findParticipants(Long id, AuthUser currentUser) {
        GymClass gymClass = getEntity(id);
        checkCanManage(gymClass, currentUser);
        return bookingRepository.findAllByGymClassIdOrderByBookedAtAsc(id).stream()
                .map(DtoMapper::toResponse).toList();
    }

    GymClass getEntity(Long id) {
        return classRepository.findById(id).orElseThrow(() -> NotFoundException.of("Zajęcia", id));
    }

    long countBooked(Long classId) {
        return bookingRepository.countByGymClassIdAndStatusIn(classId, OCCUPYING);
    }

    void checkCanManage(GymClass gymClass, AuthUser currentUser) {
        boolean isOwner = gymClass.getTrainer().getId().equals(currentUser.id());
        if (!currentUser.isAdmin() && !isOwner) {
            throw new AccessDeniedException("Możesz zarządzać tylko własnymi zajęciami");
        }
    }

    private GymClassResponse toResponse(GymClass c) {
        return DtoMapper.toResponse(c, countBooked(c.getId()));
    }

    private User resolveTrainer(GymClassRequest req, AuthUser currentUser) {
        if (currentUser.role() == Role.TRAINER) {
            return userService.getEntity(currentUser.id());
        }
        if (req.trainerId() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Wskaż trenera prowadzącego zajęcia (trainerId)");
        }
        User trainer = userService.getEntity(req.trainerId());
        if (trainer.getRole() != Role.TRAINER || !trainer.isActive()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Wybrany użytkownik nie jest aktywnym trenerem");
        }
        return trainer;
    }

    private void validateTimes(GymClassRequest req) {
        if (!req.endTime().isAfter(req.startTime())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Koniec zajęć musi być po ich rozpoczęciu");
        }
    }

    private void apply(GymClass c, GymClassRequest req) {
        c.setName(req.name().trim());
        c.setDescription(req.description());
        c.setStartTime(req.startTime());
        c.setEndTime(req.endTime());
        c.setCapacity(req.capacity());
        c.setRoom(req.room());
    }
}
