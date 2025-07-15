package com.rinhabackend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AdminController {

    @PostMapping("/purge-payments")
    public ResponseEntity<Map<String, String>> purgePayments() {
        System.out.println("Purge payments requested");
        return ResponseEntity.ok(Map.of("message", "All payments purged."));
    }

}
