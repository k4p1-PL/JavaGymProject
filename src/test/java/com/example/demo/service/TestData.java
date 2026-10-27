package com.example.demo.service;

import com.example.demo.model.GymClass;
import com.example.demo.model.Membership;
import com.example.demo.model.MembershipPlan;
import com.example.demo.model.User;
import com.example.demo.model.enums.ClassStatus;
import com.example.demo.model.enums.MembershipStatus;
import com.example.demo.model.enums.Role;

import java.math.BigDecimal;
import java.time.*;

/** Wspólne obiekty testowe dla testów jednostkowych. */
final class TestData {

    static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 2, 10, 0);
    static final Clock CLOCK = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);

    private TestData() {
    }

    static User user(long id, Role role) {
        return User.builder().id(id).email("user" + id + "@gym.pl").password("hash")
                .firstName("Imię" + id).lastName("Nazwisko" + id).role(role).active(true).build();
    }

    static MembershipPlan plan(long id, int days, Integer entries, boolean classes) {
        return MembershipPlan.builder().id(id).name("Plan " + id).durationDays(days).entryLimit(entries)
                .price(new BigDecimal("100.00")).includesClasses(classes).active(true).build();
    }

    static Membership membership(long id, User member, MembershipPlan plan, Integer remainingEntries) {
        return Membership.builder().id(id).member(member).plan(plan)
                .startDate(NOW.toLocalDate().minusDays(1)).endDate(NOW.toLocalDate().plusDays(20))
                .remainingEntries(remainingEntries).status(MembershipStatus.ACTIVE).build();
    }

    static GymClass gymClass(long id, User trainer, LocalDateTime start, int capacity) {
        return GymClass.builder().id(id).name("Joga").trainer(trainer).startTime(start)
                .endTime(start.plusHours(1)).capacity(capacity).status(ClassStatus.SCHEDULED).build();
    }
}
