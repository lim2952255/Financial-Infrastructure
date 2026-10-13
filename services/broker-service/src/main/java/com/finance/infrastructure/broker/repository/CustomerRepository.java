package com.finance.infrastructure.broker.repository;

import com.finance.infrastructure.broker.domain.customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
