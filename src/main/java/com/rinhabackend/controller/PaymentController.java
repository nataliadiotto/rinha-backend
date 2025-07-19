package com.rinhabackend.controller;

import com.rinhabackend.dto.HealthCheckResponse;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.UUID;

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

        String chosenProcessor;
        HealthCheckResponse defaultHealth = paymentProcessorService.getDefaultHealthCache();
        HealthCheckResponse fallbackHealth = paymentProcessorService.getFallbackHealthCache();

        // Strategy: Prefer DEFAULT if it's healthy, otherwise use FALLBACK if it's healthy.
        // Otherwise, fail (or choose one and hope, for initial implementation)
        if (!defaultHealth.failing()) { // Check if Default is NOT failing
            chosenProcessor = "DEFAULT";
        } else if (!fallbackHealth.failing()) {
            chosenProcessor = "FALLBACK";
        } else {
            // Both are failing. For now, let's just pick DEFAULT and let the WebClient call handle the error,
            // or you could return an error Mono immediately.
            // For simplicity of flow, let's keep the existing error handling in processPaymentService
            // but note this is where you might decide to reject the payment outright.
            System.err.println("Both processors are failing. Attempting DEFAULT anyway (will likely fail).");
            chosenProcessor = "DEFAULT"; // Or throw an exception immediately: return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
        }


        return paymentProcessorService.processPayment(
                        chosenProcessor,
                        UUID.randomUUID().toString(),
                        paymentRequest.getAmount(),
                        ISO_INSTANT_THREE_DECIMALS_FORMATTER.format(OffsetDateTime.ofInstant(Instant.now(), ZoneOffset.UTC))
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
                        payment.setProcessorType(chosenProcessor);
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

    private static final DateTimeFormatter ISO_INSTANT_THREE_DECIMALS_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .appendFraction(ChronoField.NANO_OF_SECOND, 3, 3, true) // 3 digits for nano-of-second
            .appendOffsetId() // Appends the offset, e.g., '+00:00'
            .toFormatter();

}
