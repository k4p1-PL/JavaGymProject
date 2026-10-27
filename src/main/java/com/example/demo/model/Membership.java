package com.example.demo.model;

import com.example.demo.model.enums.MembershipStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Konkretny karnet kupiony przez klubowicza.
 */
@Entity
@Table(name = "memberships")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private User member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private MembershipPlan plan;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    /** Pozostała liczba wejść (dla karnetów z limitem); null = bez limitu. */
    private Integer remainingEntries;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MembershipStatus status;

    /** Karnet jest ważny w danym dniu: aktywny, w zakresie dat i z dostępnymi wejściami. */
    public boolean isValidOn(LocalDate date) {
        return status == MembershipStatus.ACTIVE
                && !date.isBefore(startDate)
                && !date.isAfter(endDate)
                && (remainingEntries == null || remainingEntries > 0);
    }
}
