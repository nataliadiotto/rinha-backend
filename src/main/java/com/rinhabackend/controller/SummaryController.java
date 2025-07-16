package com.rinhabackend.controller;

import com.rinhabackend.dto.PaymentSummaryResponse;
import com.rinhabackend.dto.SummaryData;
import com.rinhabackend.model.Payment;
import com.rinhabackend.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
public class SummaryController {

    private final PaymentRepository paymentRepository;

    @Autowired
    public SummaryController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @GetMapping("/payments-summary")
    public ResponseEntity<PaymentSummaryResponse>  getPaymentSummary() {
        System.out.println("GET /payments-summary requested.");

        // Retrieve all payments from the database
        Iterable<Payment> allPaymentsIterable = paymentRepository.findAll();

        Map<String, SummaryData> summaryMap = StreamSupport.stream(allPaymentsIterable.spliterator(), false)
                .collect(Collectors.groupingBy(
                        Payment::getProcessorType, // Group by processorType ("DEFAULT" or "FALLBACK")
                        Collectors.reducing(
                                new SummaryData(0L, BigDecimal.ZERO), // Initial accumulator
                                payment -> new SummaryData(1L, payment.getAmount()), // Mapper: transform each payment into a SummaryData (count=1, amount)
                                (sd1, sd2) -> new SummaryData( // Combiner: merge two SummaryData objects
                                        sd1.getTotalRequests() + sd2.getTotalRequests(),
                                        sd1.getTotalAmount().add(sd2.getTotalAmount())
                                )
                        )
                ));

        // Get the summary data for each type, handling cases where no payments for a type exist
        SummaryData defaultSummary = summaryMap.getOrDefault("DEFAULT", new SummaryData(0L, BigDecimal.ZERO));
        SummaryData fallbackSummary = summaryMap.getOrDefault("FALLBACK", new SummaryData(0L, BigDecimal.ZERO));

        // Ensure amounts are scaled to 2 decimal places before returning
        defaultSummary.setTotalAmount(defaultSummary.getTotalAmount().setScale(2, BigDecimal.ROUND_HALF_UP));
        fallbackSummary.setTotalAmount(fallbackSummary.getTotalAmount().setScale(2, BigDecimal.ROUND_HALF_UP));

        PaymentSummaryResponse response = new PaymentSummaryResponse(defaultSummary, fallbackSummary);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
 }


