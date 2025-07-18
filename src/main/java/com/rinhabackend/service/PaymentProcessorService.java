package com.rinhabackend.service;

import com.rinhabackend.dto.ExternalPaymentRequest;
import com.rinhabackend.dto.ExternalPaymentResponse;
import com.rinhabackend.dto.HealthCheckResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class PaymentProcessorService {

    private final WebClient webClientDefault;
    private final WebClient webClientFallback;
    private volatile HealthCheckResponse defaultHealthCache;
    private volatile HealthCheckResponse fallbackHealthCache;
    private volatile Instant lastDefaultCheckTime;
    private volatile Instant lastFallbackCheckTime;


    @Autowired
    public PaymentProcessorService(WebClient.Builder webClientBuilder) {
        //this.webClientDefault = webClientBuilder.baseUrl("http://payment-processor-default:8080").build();
        //this.webClientFallback = webClientBuilder.baseUrl("http://payment-processor-fallback:8080").build();

        this.webClientDefault = webClientBuilder.baseUrl("http://localhost:8001").build();
        this.webClientFallback = webClientBuilder.baseUrl("http://localhost:8002").build();

        this.defaultHealthCache = new HealthCheckResponse(true, Integer.MAX_VALUE);
        this.fallbackHealthCache = new HealthCheckResponse(true, Integer.MAX_VALUE);
        this.lastDefaultCheckTime = Instant.MIN;
        this.lastFallbackCheckTime = Instant.MIN;

    }

    public Mono<ExternalPaymentResponse> processPayment(String processorType, String correlationId, BigDecimal amount, Instant requestedAt) {
        ExternalPaymentRequest requestBody = new ExternalPaymentRequest(correlationId, amount, requestedAt);

        WebClient targetClient;
        if ("DEFAULT".equalsIgnoreCase(processorType)) {
           targetClient = webClientDefault;
           System.out.println("Forwarding payment to DEFAULT processor: " + correlationId);
        } else if ("FALLBACK".equalsIgnoreCase(processorType)) {
           targetClient = webClientFallback;
           System.out.println("Forwarding payment to FALLBACK processor: " + correlationId);
        } else {
           System.err.println("Invalid processor type received: " + processorType);
           return Mono.error(new IllegalArgumentException("Invalid processor type: " + processorType));
        }

        return targetClient.post()
                .uri("/payments") // Endpoint path on the external processor
                .bodyValue(requestBody)
                .retrieve() // Initiate the request and retrieve the response
                // For 4xx errors
                .onStatus(status -> status.is4xxClientError(), clientResponse ->
                        Mono.error(new RuntimeException("Client error from processor: " + clientResponse.statusCode()))) // For 4xx errors
                .onStatus(status -> status.is5xxServerError(), clientResponse ->
                        Mono.error(new RuntimeException("Server error from processor: " + clientResponse.statusCode()))) // Handle 5xx server errors
                .bodyToMono(ExternalPaymentResponse.class); // Convert the response body to ExternalPaymentResponse Mono

    }

    public Mono<HealthCheckResponse> getProcessorHealth(String processorType) {
        WebClient targetClient;
        String logMessage;

        if ("DEFAULT".equalsIgnoreCase(processorType)) {
            targetClient = webClientDefault;
            logMessage = "Checking health of DEFAULT processor.";
        } else if ("FALLBACK".equalsIgnoreCase(processorType)) {
            targetClient = webClientFallback;
            logMessage = "Checking health of FALLBACK processor.";
        } else  {
            System.err.println("Invalid processor type received: " + processorType);
            return Mono.error(new IllegalArgumentException("Invalid processor type for health check: " + processorType));
        }

        System.out.println(logMessage);

        //Build and execute the WebClient GET request
        return targetClient.get() // Start building a GET request
                .uri("/payments/service-health") // The specific endpoint path
                .retrieve() // Initiate the request and retrieve the response
                .onStatus(status -> status.isError(), clientResponse -> { // Handle any error status (4xx, 5xx)
                    System.err.println("Error getting health from processor: " + clientResponse.statusCode());

                    return Mono.error(new RuntimeException("Failed to get health from processor: " + clientResponse.statusCode()));
                })
                .bodyToMono(HealthCheckResponse.class); // Convert the response body to HealthCheckResponse Mono
    }

    @Scheduled(fixedRate = 5000)
    public void updateHealthCaches() {
        System.out.println("Scheduled health check initiated.");

        getProcessorHealth("DEFAULT")
                .subscribe(
                        health -> {
                            defaultHealthCache = health;
                            lastDefaultCheckTime = Instant.now();
                            System.out.println("DEFAULT Health Updated: " + health);
                        },
                        error -> {
                            defaultHealthCache = new HealthCheckResponse(true, Integer.MAX_VALUE);
                            lastDefaultCheckTime = Instant.now();
                            System.err.println("Error checking DEFAULT health: " + error.getMessage());
                        });

        getProcessorHealth("FALLBACK")
                .subscribe(
                        health -> {
                            defaultHealthCache = health;
                            lastFallbackCheckTime = Instant.now();
                            System.out.println("FALLBACK Health Updated: " + health);
                        },
                        error -> {
                            defaultHealthCache = new HealthCheckResponse(true, Integer.MAX_VALUE);
                            lastFallbackCheckTime = Instant.now();
                            System.err.println("Error checking FALLBACK Health: " + error.getMessage());
                        });

    }

    public HealthCheckResponse getDefaultHealthCache() {
        return defaultHealthCache;
    }

    public HealthCheckResponse getFallbackHealthCache() {
        return fallbackHealthCache;
    }




}
