package com.rinhabackend.controller;

import com.rinhabackend.dto.PaymentRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    @PostMapping
    public ResponseEntity<Void> processPayment(@RequestBody PaymentRequest paymentRequest) {
        System.out.println("Received payment:" + paymentRequest.getCorrelationId() + " amount " + paymentRequest.getAmount());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();

    }


}
