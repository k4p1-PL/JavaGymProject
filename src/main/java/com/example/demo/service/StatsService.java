package com.example.demo.service;

import com.example.demo.dto.StatsResponse;
import com.example.demo.model.enums.ClassStatus;
import com.example.demo.model.enums.MembershipStatus;
import com.example.demo.model.enums.PaymentStatus;
import com.example.demo.model.enums.Role;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final GymClassRepository classRepository;
    private final CheckInRepository checkInRepository;
    private final PaymentRepository paymentRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public StatsResponse getStats() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        return new StatsResponse(
                userRepository.countByRoleAndActiveTrue(Role.MEMBER),
                userRepository.countByRoleAndActiveTrue(Role.TRAINER),
                membershipRepository.countByStatus(MembershipStatus.ACTIVE),
                classRepository.countByStatusAndStartTimeAfter(ClassStatus.SCHEDULED, now),
                checkInRepository.countByCheckInTimeAfter(today.atStartOfDay()),
                checkInRepository.countByCheckOutTimeIsNull(),
                paymentRepository.sumAmountSince(PaymentStatus.COMPLETED, today.withDayOfMonth(1).atStartOfDay())
        );
    }
}
