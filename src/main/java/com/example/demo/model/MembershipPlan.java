package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Rodzaj karnetu oferowany przez siłownię (np. "Open miesięczny", "Studencki", "10 wejść").
 */
@Entity
@Table(name = "membership_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Integer durationDays;

    /** Limit wejść w okresie karnetu; null = bez limitu. */
    private Integer entryLimit;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** Czy karnet obejmuje zajęcia grupowe. */
    @Builder.Default
    @Column(nullable = false)
    private boolean includesClasses = true;

    /** Czy plan jest jeszcze w sprzedaży. */
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
