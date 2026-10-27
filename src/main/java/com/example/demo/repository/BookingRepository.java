package com.example.demo.repository;

import com.example.demo.model.Booking;
import com.example.demo.model.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    long countByGymClassIdAndStatusIn(Long gymClassId, Collection<BookingStatus> statuses);

    boolean existsByMemberIdAndGymClassIdAndStatusIn(Long memberId, Long gymClassId, Collection<BookingStatus> statuses);

    List<Booking> findAllByMemberIdOrderByBookedAtDesc(Long memberId);

    List<Booking> findAllByGymClassIdOrderByBookedAtAsc(Long gymClassId);

    List<Booking> findAllByGymClassIdAndStatus(Long gymClassId, BookingStatus status);

    boolean existsByGymClassId(Long gymClassId);
}
