package com.rinhabackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentSummaryResponse {

    private SummaryData Default;
    private SummaryData Fallback;
}


