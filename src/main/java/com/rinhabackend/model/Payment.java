package com.rinhabackend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table("payments")
public class Payment {

    @Id
    private Long id;

    @Column("correlation_id")
    private String correlationId;

    private BigDecimal amount;

    @Column("processor_type")
    private String processorType;

    @Column("processed_at")
    LocalDateTime processedAt;

}
