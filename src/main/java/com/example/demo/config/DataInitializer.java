package com.example.demo.config;

import com.example.demo.model.*;
import com.example.demo.model.enums.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Przy pierwszym uruchomieniu (pusta tabela users) zakłada konta demonstracyjne,
 * ofertę karnetów i grafik zajęć na najbliższy tydzień.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final MembershipPlanRepository planRepository;
    private final MembershipRepository membershipRepository;
    private final PaymentRepository paymentRepository;
    private final GymClassRepository classRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed-demo-data:true}")
    private boolean seedDemoData;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedDemoData || userRepository.count() > 0) {
            return;
        }
        log.info("Pusta baza – tworzę dane demonstracyjne");

        userRepository.save(user("admin@gym.pl", "admin123", "Anna", "Administratorka", Role.ADMIN));
        User trainer1 = userRepository.save(user("trener@gym.pl", "trener123", "Tomasz", "Trenerski", Role.TRAINER));
        User trainer2 = userRepository.save(user("joga@gym.pl", "trener123", "Ewa", "Spokojna", Role.TRAINER));
        User member = userRepository.save(user("jan@gym.pl", "klient123", "Jan", "Kowalski", Role.MEMBER));
        userRepository.save(user("ola@gym.pl", "klient123", "Aleksandra", "Nowak", Role.MEMBER));

        MembershipPlan open = planRepository.save(plan("Open miesięczny", "Nielimitowany dostęp do siłowni i zajęć grupowych", 30, null, "149.00", true));
        planRepository.save(plan("Poranny", "Wejścia w godzinach 6:00–14:00, bez zajęć grupowych", 30, null, "99.00", false));
        planRepository.save(plan("Karnet 10 wejść", "10 wejść do wykorzystania w ciągu 60 dni", 60, 10, "120.00", true));
        planRepository.save(plan("Roczny", "Nielimitowany dostęp przez 12 miesięcy", 365, null, "1299.00", true));

        Membership membership = membershipRepository.save(Membership.builder()
                .member(member).plan(open)
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().plusDays(24))
                .status(MembershipStatus.ACTIVE)
                .build());
        paymentRepository.save(Payment.builder()
                .membership(membership).amount(open.getPrice())
                .method(PaymentMethod.CARD).status(PaymentStatus.COMPLETED)
                .paidAt(LocalDateTime.now().minusDays(5))
                .build());

        LocalDate today = LocalDate.now();
        for (int day = 1; day <= 7; day++) {
            LocalDate date = today.plusDays(day);
            classRepository.save(gymClass("Trening obwodowy", "Intensywny trening całego ciała", trainer1, date, 18, 60, 15, "Sala 1"));
            classRepository.save(gymClass("Joga", "Zajęcia dla początkujących i średniozaawansowanych", trainer2, date, 19, 75, 12, "Sala 2"));
            if (day % 2 == 0) {
                classRepository.save(gymClass("Zdrowy kręgosłup", "Ćwiczenia wzmacniające mięśnie głębokie", trainer2, date, 10, 45, 10, "Sala 2"));
            }
        }
        log.info("Konta demo: admin@gym.pl/admin123, trener@gym.pl/trener123, jan@gym.pl/klient123");
    }

    private User user(String email, String password, String firstName, String lastName, Role role) {
        return User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .role(role)
                .active(true)
                .build();
    }

    private MembershipPlan plan(String name, String description, int days, Integer entries, String price, boolean classes) {
        return MembershipPlan.builder()
                .name(name).description(description).durationDays(days).entryLimit(entries)
                .price(new BigDecimal(price)).includesClasses(classes).active(true)
                .build();
    }

    private GymClass gymClass(String name, String description, User trainer, LocalDate date,
                              int hour, int minutes, int capacity, String room) {
        LocalDateTime start = date.atTime(LocalTime.of(hour, 0));
        return GymClass.builder()
                .name(name).description(description).trainer(trainer)
                .startTime(start).endTime(start.plusMinutes(minutes))
                .capacity(capacity).room(room).status(ClassStatus.SCHEDULED)
                .build();
    }
}
