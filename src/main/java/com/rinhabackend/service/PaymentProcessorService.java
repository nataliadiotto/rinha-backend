package com.rinhabackend.service;

import com.rinhabackend.dto.ExternalPaymentRequest;
import com.rinhabackend.dto.ExternalPaymentResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class PaymentProcessorService {

    private final WebClient webClientDefault;
    private final WebClient webClientFallback;

    @Autowired
    public PaymentProcessorService(WebClient.Builder webClientBuilder) {
        this.webClientDefault = webClientBuilder.baseUrl("http://payment-processor-default:8080").build();
        this.webClientFallback = webClientBuilder.baseUrl("http://payment-processor-fallback:8080").build();
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

}
