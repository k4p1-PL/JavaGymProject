package com.example.demo.repository;

import com.example.demo.model.GymClass;
import com.example.demo.model.enums.ClassStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface GymClassRepository extends JpaRepository<GymClass, Long> {

    List<GymClass> findAllByStartTimeBetweenOrderByStartTimeAsc(LocalDateTime from, LocalDateTime to);

    List<GymClass> findAllByTrainerIdOrderByStartTimeDesc(Long trainerId);

    boolean existsByNameIgnoreCaseAndStartTimeAndTrainerId(String name, LocalDateTime startTime, Long trainerId);

    long countByStatusAndStartTimeAfter(ClassStatus status, LocalDateTime time);
}
