package com.rinhabackend.controller;

import com.rinhabackend.dto.PaymentRequest;
import com.rinhabackend.model.Payment;
import com.rinhabackend.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentRepository paymentRepository;

    @Autowired
    public PaymentController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @PostMapping
    public ResponseEntity<Void> processPayment(@RequestBody PaymentRequest paymentRequest) {
        System.out.println("Received payment:" + paymentRequest.getCorrelationId() + " amount " + paymentRequest.getAmount());

        Payment payment = new Payment();
        payment.setCorrelationId(paymentRequest.getCorrelationId());
        payment.setAmount(paymentRequest.getAmount());
        payment.setProcessorType("DEFAULT");
        payment.setProcessedAt(LocalDateTime.now());

        paymentRepository.save(payment);
        System.out.println("Payment saved to database with correlation ID: " + payment.getCorrelationId());

        return ResponseEntity.status(HttpStatus.ACCEPTED).build();

    }


}
