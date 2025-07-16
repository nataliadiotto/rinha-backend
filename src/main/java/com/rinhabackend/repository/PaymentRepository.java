package com.rinhabackend.repository;

import com.rinhabackend.model.Payment;
import org.springframework.data.repository.CrudRepository;

public interface PaymentRepository extends CrudRepository<Payment, Long> {


}
