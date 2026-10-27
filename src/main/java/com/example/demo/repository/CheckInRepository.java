package com.example.demo.repository;

import com.example.demo.model.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CheckInRepository extends JpaRepository<CheckIn, Long> {

    List<CheckIn> findAllByMemberIdOrderByCheckInTimeDesc(Long memberId);

    List<CheckIn> findTop100ByOrderByCheckInTimeDesc();

    Optional<CheckIn> findFirstByMemberIdAndCheckOutTimeIsNull(Long memberId);

    long countByCheckInTimeAfter(LocalDateTime time);

    long countByCheckOutTimeIsNull();
}
