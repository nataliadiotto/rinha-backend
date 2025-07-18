package com.rinhabackend.dto;

public record HealthCheckResponse(boolean failing, int minResponseTime) {

}
