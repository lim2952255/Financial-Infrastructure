package com.finance.infrastructure.bank.repository;

import com.finance.infrastructure.bank.domain.customer.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findFirstByNameOrderByIdAsc(String name);
}
