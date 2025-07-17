package com.rinhabackend.controller;

import com.rinhabackend.dto.PaymentRequest;
import com.rinhabackend.model.Payment;
import com.rinhabackend.repository.PaymentRepository;
import com.rinhabackend.service.PaymentProcessorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentProcessorService paymentProcessorService;
    private final PaymentRepository paymentRepository;

    @Autowired
    public PaymentController(PaymentProcessorService paymentProcessorService, PaymentRepository paymentRepository) {
        this.paymentProcessorService = paymentProcessorService;
        this.paymentRepository = paymentRepository;
    }

    @PostMapping
    public Mono<ResponseEntity<Object>> processPayment(@RequestBody PaymentRequest paymentRequest) {
        System.out.println("Received payment: " + paymentRequest.getCorrelationId() + " amount " + paymentRequest.getAmount());

        String processorType = "DEFAULT";

        return paymentProcessorService.processPayment(
                        processorType,
                        paymentRequest.getCorrelationId(),
                        paymentRequest.getAmount(),
                        Instant.now()
                )
                .flatMap(externalResponse -> {
                    System.out.println("External processor response: " + externalResponse.message());

                    // IMPORTANT: paymentRepository.save() is a BLOCKING call from Spring Data JDBC.
                    // If you're returning a Mono from the controller, you should generally avoid blocking
                    // within the reactive chain. For learning, we'll use subscribeOn
                    // to move this blocking operation off the main event loop thread.
                    return Mono.fromCallable(() -> {
                        Payment payment = new Payment();
                        payment.setCorrelationId(paymentRequest.getCorrelationId());
                        payment.setAmount(paymentRequest.getAmount());
                        payment.setProcessorType(processorType);
                        payment.setProcessedAt(LocalDateTime.now());
                        paymentRepository.save(payment); // This is the blocking call
                        System.out.println("Payment saved for correlationId: " + payment.getCorrelationId());
                        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
                    }).subscribeOn(Schedulers.boundedElastic()); // Run blocking DB call on a different thread pool
                })
                .onErrorResume(RuntimeException.class, e -> {
                    System.err.println("Error processing payment externally: " + e.getMessage());
                    // Error response: returns ResponseEntity<Object> now
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
                });
    }

}
