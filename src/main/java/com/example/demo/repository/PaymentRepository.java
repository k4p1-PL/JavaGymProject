package com.example.demo.repository;

import com.example.demo.model.Payment;
import com.example.demo.model.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findAllByMembershipMemberIdOrderByPaidAtDesc(Long memberId);

    List<Payment> findAllByOrderByPaidAtDesc();

    List<Payment> findAllByMembershipId(Long membershipId);

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.status = :status and p.paidAt >= :from")
    BigDecimal sumAmountSince(@Param("status") PaymentStatus status, @Param("from") LocalDateTime from);
}
