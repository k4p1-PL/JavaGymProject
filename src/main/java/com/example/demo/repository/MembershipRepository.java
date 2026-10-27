package com.example.demo.repository;

import com.example.demo.model.Membership;
import com.example.demo.model.enums.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MembershipRepository extends JpaRepository<Membership, Long> {

    List<Membership> findAllByMemberIdOrderByStartDateDesc(Long memberId);

    List<Membership> findAllByOrderByStartDateDesc();

    List<Membership> findAllByMemberIdAndStatus(Long memberId, MembershipStatus status);

    boolean existsByPlanId(Long planId);

    long countByStatus(MembershipStatus status);

    /** Oznacza jako wygasłe wszystkie aktywne karnety, których data końca minęła. */
    @Modifying
    @Query("update Membership m set m.status = :expired where m.status = :active and m.endDate < :today")
    int expireOutdated(@Param("today") LocalDate today,
                       @Param("active") MembershipStatus active,
                       @Param("expired") MembershipStatus expired);
}
