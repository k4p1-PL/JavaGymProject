package com.example.demo.service;

import com.example.demo.dto.DtoMapper;
import com.example.demo.dto.PaymentResponse;
import com.example.demo.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public List<PaymentResponse> findAll() {
        return paymentRepository.findAllByOrderByPaidAtDesc().stream().map(DtoMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> findForUser(Long userId) {
        return paymentRepository.findAllByMembershipMemberIdOrderByPaidAtDesc(userId).stream()
                .map(DtoMapper::toResponse).toList();
    }
}
