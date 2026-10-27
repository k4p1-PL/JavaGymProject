package com.example.demo.dto;

import com.example.demo.model.*;

import java.time.LocalDate;

/** Mapowanie encji na obiekty DTO zwracane przez API. */
public final class DtoMapper {

    private DtoMapper() {
    }

    public static UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getFirstName(), u.getLastName(),
                u.getPhoneNumber(), u.getDateOfBirth(), u.getRole(), u.isActive(), u.getCreatedAt());
    }

    public static PlanResponse toResponse(MembershipPlan p) {
        return new PlanResponse(p.getId(), p.getName(), p.getDescription(), p.getDurationDays(),
                p.getEntryLimit(), p.getPrice(), p.isIncludesClasses(), p.isActive());
    }

    public static MembershipResponse toResponse(Membership m) {
        User u = m.getMember();
        MembershipPlan p = m.getPlan();
        return new MembershipResponse(m.getId(), u.getId(), u.getFullName(), u.getEmail(),
                p.getId(), p.getName(), p.isIncludesClasses(), m.getStartDate(), m.getEndDate(),
                m.getRemainingEntries(), m.getStatus(), m.isValidOn(LocalDate.now()));
    }

    public static PaymentResponse toResponse(Payment p) {
        Membership m = p.getMembership();
        return new PaymentResponse(p.getId(), m.getId(), m.getMember().getId(), m.getMember().getFullName(),
                m.getPlan().getName(), p.getAmount(), p.getMethod(), p.getStatus(), p.getPaidAt());
    }

    public static GymClassResponse toResponse(GymClass c, long bookedCount) {
        return new GymClassResponse(c.getId(), c.getName(), c.getDescription(),
                c.getTrainer().getId(), c.getTrainer().getFullName(), c.getStartTime(), c.getEndTime(),
                c.getCapacity(), bookedCount, Math.max(0, c.getCapacity() - bookedCount),
                c.getRoom(), c.getStatus());
    }

    public static BookingResponse toResponse(Booking b) {
        GymClass c = b.getGymClass();
        return new BookingResponse(b.getId(), b.getMember().getId(), b.getMember().getFullName(),
                c.getId(), c.getName(), c.getStartTime(), c.getTrainer().getFullName(), c.getRoom(),
                b.getBookedAt(), b.getStatus());
    }

    public static CheckInResponse toResponse(CheckIn ci) {
        Membership m = ci.getMembership();
        return new CheckInResponse(ci.getId(), ci.getMember().getId(), ci.getMember().getFullName(),
                m.getId(), m.getPlan().getName(), m.getRemainingEntries(),
                ci.getCheckInTime(), ci.getCheckOutTime());
    }
}
