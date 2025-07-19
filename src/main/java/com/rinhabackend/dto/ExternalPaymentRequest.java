package com.rinhabackend.dto;

import java.math.BigDecimal;

public record ExternalPaymentRequest(String correlationId, BigDecimal amount, String requestedAt) {}


