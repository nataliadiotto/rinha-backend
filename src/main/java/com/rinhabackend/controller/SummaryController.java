package com.rinhabackend.controller;

import com.rinhabackend.dto.PaymentSummaryResponse;
import com.rinhabackend.dto.SummaryData;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Random;

@RestController
public class SummaryController {

    @GetMapping("/payments-summary")
    public ResponseEntity<PaymentSummaryResponse>  getPaymentSummary() {
        System.out.println("GET /payments-summary requested.");

        Random random = new Random();
        long defaultRequests = random.nextInt(1000) + 1; // Random number between 1 and 1000
        BigDecimal defaultAmount = BigDecimal.valueOf(random.nextDouble() * 10000).setScale(2, BigDecimal.ROUND_HALF_UP);

        SummaryData defaultSummary = new SummaryData(defaultRequests, defaultAmount);

        // --- Create dummy data for 'fallback' processor ---
        long fallbackRequests = random.nextInt(500) + 1; // Random number between 1 and 500
        BigDecimal fallbackAmount = BigDecimal.valueOf(random.nextDouble() * 5000).setScale(2, BigDecimal.ROUND_HALF_UP);

        SummaryData fallbackSummary = new SummaryData(fallbackRequests, fallbackAmount);

        // --- Create the full response object ---
        PaymentSummaryResponse response = new PaymentSummaryResponse(defaultSummary, fallbackSummary);

        // --- Return the response with HTTP 200 OK status ---
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

}
