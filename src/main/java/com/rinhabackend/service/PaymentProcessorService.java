package com.rinhabackend.service;

import com.rinhabackend.dto.ExternalPaymentRequest;
import com.rinhabackend.dto.ExternalPaymentResponse;
import com.rinhabackend.dto.HealthCheckResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class PaymentProcessorService {

    private static final Logger log = LoggerFactory.getLogger(PaymentProcessorService.class);

    private final WebClient webClientDefault;
    private final WebClient webClientFallback;

    // Cache fields for health checks (volatile for thread visibility)
    private volatile HealthCheckResponse defaultHealthCache;
    private volatile HealthCheckResponse fallbackHealthCache;
    private volatile Instant lastDefaultHealthCheckTime;
    private volatile Instant lastFallbackHealthCheckTime;


    @Autowired
    public PaymentProcessorService(WebClient.Builder webClientBuilder) {
        // Using localhost for native app testing
        this.webClientDefault = webClientBuilder.baseUrl("http://localhost:8001").build();
        this.webClientFallback = webClientBuilder.baseUrl("http://localhost:8002").build();

        // Initialize cache fields to a safe, 'unhealthy' default state for startup
        this.defaultHealthCache = new HealthCheckResponse(true, Integer.MAX_VALUE);
        this.fallbackHealthCache = new HealthCheckResponse(true, Integer.MAX_VALUE);
        this.lastDefaultHealthCheckTime = Instant.MIN; // Ensures immediate check on startup
        this.lastFallbackHealthCheckTime = Instant.MIN; // Ensures immediate check on startup
    }

    /**
     * Sends a payment request to the specified external payment processor.
     * Returns a Mono that will emit the ExternalPaymentResponse upon success, or an error.
     *
     * @param processorType   "DEFAULT" or "FALLBACK"
     * @param correlationId   Unique ID for the payment
     * @param amount          Amount of the payment
     * @param requestedAt     Timestamp of the request in ISO_INSTANT string format
     * @return Mono<ExternalPaymentResponse> representing the outcome of the request
     */
    public Mono<ExternalPaymentResponse> processPayment(
            String processorType,
            String correlationId,
            BigDecimal amount,
            String requestedAt) {

        ExternalPaymentRequest requestBody = new ExternalPaymentRequest(correlationId, amount, requestedAt);

        log.info("Outgoing ExternalPaymentRequest: {}", requestBody);

        WebClient targetClient;
        if ("DEFAULT".equalsIgnoreCase(processorType)) {
            targetClient = webClientDefault;
            log.info("Forwarding payment to DEFAULT processor: {}", correlationId);
        } else if ("FALLBACK".equalsIgnoreCase(processorType)) {
            targetClient = webClientFallback;
            log.info("Forwarding payment to FALLBACK processor: {}", correlationId);
        } else {
            log.error("Invalid processor type received: {}", processorType);
            return Mono.error(new IllegalArgumentException("Invalid processor type: " + processorType));
        }

        return targetClient.post()
                .uri("/payments")
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), clientResponse -> {
                    log.error("Client error from processor {}: {}", processorType, clientResponse.statusCode());
                    return Mono.error(new RuntimeException("Client error from processor: " + clientResponse.statusCode()));
                })
                .onStatus(status -> status.is5xxServerError(), clientResponse -> {
                    log.error("Server error from processor {}: {}", processorType, clientResponse.statusCode());
                    return Mono.error(new RuntimeException("Server error from processor: " + clientResponse.statusCode()));
                })
                .bodyToMono(ExternalPaymentResponse.class)
                .timeout(java.time.Duration.ofSeconds(2))
                .retry(1);
    }

    /**
     * Checks the health of a specific external payment processor.
     * Returns a Mono that will emit the HealthCheckResponse upon success, or an error.
     *
     * @param processorType "DEFAULT" or "FALLBACK"
     * @return Mono<HealthCheckResponse> representing the health status
     */
    public Mono<HealthCheckResponse> getProcessorHealth(String processorType) {
        WebClient targetClient;

        if ("DEFAULT".equalsIgnoreCase(processorType)) {
            targetClient = webClientDefault;
            log.info("Checking health of DEFAULT processor.");
        } else if ("FALLBACK".equalsIgnoreCase(processorType)) {
            targetClient = webClientFallback;
            log.info("Checking health of FALLBACK processor.");
        } else {
            log.error("Invalid processor type for health check: {}", processorType);
            return Mono.error(new IllegalArgumentException("Invalid processor type for health check: " + processorType));
        }

        // Build and execute the WebClient GET request
        return targetClient.get()
                .uri("/payments/service-health")
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse -> {
                    log.error("Error getting health from processor {}: {}", processorType, clientResponse.statusCode());
                    return Mono.error(new RuntimeException("Failed to get health from processor: " + clientResponse.statusCode()));
                })
                .bodyToMono(HealthCheckResponse.class)
                .timeout(java.time.Duration.ofSeconds(2))
                .retry(1);
    }

    /**
     * Scheduled method to periodically update the cached health status of both processors.
     * Runs every 5 seconds.
     */
    @Scheduled(fixedRate = 5000)
    public void updateHealthCaches() { // No parameters for @Scheduled
        log.info("Scheduled health check initiated.");

        // Check DEFAULT processor health
        getProcessorHealth("DEFAULT")
                .subscribe(
                        health -> { //onNext: success handler
                            defaultHealthCache = health;
                            lastDefaultHealthCheckTime = Instant.now();
                            log.info("DEFAULT Health Updated: {}", health);
                        },
                        error -> { //onError: error handler
                            defaultHealthCache = new HealthCheckResponse(true, Integer.MAX_VALUE);
                            lastDefaultHealthCheckTime = Instant.now();
                            log.error("Error checking DEFAULT health: {}", error.getMessage());
                        }
                );

        // Check FALLBACK processor health
        getProcessorHealth("FALLBACK")
                .subscribe(
                        health -> { // onNext: success handler
                            fallbackHealthCache = health;
                            lastFallbackHealthCheckTime = Instant.now();
                            log.info("FALLBACK Health Updated: {}", health);
                        },
                        error -> { // onError: error handler
                            fallbackHealthCache = new HealthCheckResponse(true, Integer.MAX_VALUE);
                            lastFallbackHealthCheckTime = Instant.now();
                            log.error("Error checking FALLBACK health: {}", error.getMessage());
                        }
                );
    }

    /**
     * Provides access to the current cached health status of the default processor.
     * @return The last known HealthCheckResponse for the default processor.
     */
    public HealthCheckResponse getDefaultHealthCache() {
        return defaultHealthCache;
    }

    /**
     * Provides access to the current cached health status of the fallback processor.
     * @return The last known HealthCheckResponse for the fallback processor.
     */
    public HealthCheckResponse getFallbackHealthCache() {
        return fallbackHealthCache;
    }
}