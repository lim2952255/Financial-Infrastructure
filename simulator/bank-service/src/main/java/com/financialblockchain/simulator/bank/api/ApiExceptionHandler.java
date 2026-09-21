package com.financialblockchain.simulator.bank.api;

import com.financialblockchain.simulator.bank.api.BankApiModels.ErrorResponse;
import com.financialblockchain.simulator.bank.support.AccountNotFoundException;
import com.financialblockchain.simulator.bank.support.DuplicateAccountException;
import com.financialblockchain.simulator.bank.support.DuplicateTransactionException;
import com.financialblockchain.simulator.bank.support.InsufficientBalanceException;
import com.financialblockchain.simulator.bank.support.SimulatedCreditFailureException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(AccountNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler(DuplicateAccountException.class)
    ResponseEntity<ErrorResponse> duplicateAccount(DuplicateAccountException exception) {
        return response(HttpStatus.CONFLICT, "DUPLICATE_ACCOUNT", exception.getMessage());
    }

    @ExceptionHandler(DuplicateTransactionException.class)
    ResponseEntity<ErrorResponse> duplicate(DuplicateTransactionException exception) {
        return response(HttpStatus.CONFLICT, "DUPLICATE_TRANSACTION", exception.getMessage());
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    ResponseEntity<ErrorResponse> insufficientBalance(InsufficientBalanceException exception) {
        return response(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_BALANCE", exception.getMessage());
    }

    @ExceptionHandler(SimulatedCreditFailureException.class)
    ResponseEntity<ErrorResponse> simulatedFailure(SimulatedCreditFailureException exception) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "SIMULATED_CREDIT_FAILURE", exception.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ResponseEntity<ErrorResponse> badRequest(Exception exception) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage());
    }

    private static ResponseEntity<ErrorResponse> response(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(code, message, Instant.now()));
    }
}
