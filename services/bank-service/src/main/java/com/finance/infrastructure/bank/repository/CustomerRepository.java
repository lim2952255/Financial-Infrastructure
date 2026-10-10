package com.finance.infrastructure.bank.repository;

import com.finance.infrastructure.bank.domain.customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
