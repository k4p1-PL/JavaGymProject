package com.example.demo.service;

import com.example.demo.dto.CheckInRequest;
import com.example.demo.dto.CheckInResponse;
import com.example.demo.dto.DtoMapper;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.model.CheckIn;
import com.example.demo.model.Membership;
import com.example.demo.model.User;
import com.example.demo.repository.CheckInRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckInService {

    private final CheckInRepository checkInRepository;
    private final UserRepository userRepository;
    private final MembershipService membershipService;
    private final Clock clock;

    /** Rejestracja wejścia na recepcji: sprawdza karnet i zużywa jedno wejście (dla karnetów z limitem). */
    @Transactional
    public CheckInResponse checkIn(CheckInRequest req) {
        User member = findMember(req);
        if (!member.isActive()) {
            throw new BusinessException("Konto klubowicza jest zablokowane");
        }
        if (checkInRepository.findFirstByMemberIdAndCheckOutTimeIsNull(member.getId()).isPresent()) {
            throw new BusinessException(member.getFullName() + " jest już na siłowni (brak zarejestrowanego wyjścia)");
        }
        Membership membership = membershipService.findValidMembership(member.getId(), false)
                .orElseThrow(() -> new BusinessException(member.getFullName() + " nie ma ważnego karnetu"));
        if (membership.getRemainingEntries() != null) {
            membership.setRemainingEntries(membership.getRemainingEntries() - 1);
        }
        CheckIn checkIn = CheckIn.builder()
                .member(member)
                .membership(membership)
                .checkInTime(LocalDateTime.now(clock))
                .build();
        return DtoMapper.toResponse(checkInRepository.save(checkIn));
    }

    @Transactional
    public CheckInResponse checkOut(Long checkInId) {
        CheckIn checkIn = checkInRepository.findById(checkInId)
                .orElseThrow(() -> NotFoundException.of("Wejście", checkInId));
        if (checkIn.getCheckOutTime() != null) {
            throw new BusinessException("Wyjście zostało już zarejestrowane");
        }
        checkIn.setCheckOutTime(LocalDateTime.now(clock));
        return DtoMapper.toResponse(checkIn);
    }

    @Transactional(readOnly = true)
    public List<CheckInResponse> findRecent() {
        return checkInRepository.findTop100ByOrderByCheckInTimeDesc().stream().map(DtoMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CheckInResponse> findForUser(Long userId) {
        return checkInRepository.findAllByMemberIdOrderByCheckInTimeDesc(userId).stream()
                .map(DtoMapper::toResponse).toList();
    }

    private User findMember(CheckInRequest req) {
        if (req.userId() != null) {
            return userRepository.findById(req.userId())
                    .orElseThrow(() -> NotFoundException.of("Użytkownik", req.userId()));
        }
        if (req.email() != null && !req.email().isBlank()) {
            return userRepository.findByEmailIgnoreCase(req.email().trim())
                    .orElseThrow(() -> new NotFoundException("Brak użytkownika z adresem " + req.email()));
        }
        throw new BusinessException(HttpStatus.BAD_REQUEST, "Podaj userId albo email klubowicza");
    }
}
