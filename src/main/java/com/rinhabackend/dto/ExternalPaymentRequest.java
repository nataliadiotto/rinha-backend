package com.rinhabackend.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ExternalPaymentRequest(String correlationId,
                                     BigDecimal amount,
                                     Instant requestedAt) {

}
