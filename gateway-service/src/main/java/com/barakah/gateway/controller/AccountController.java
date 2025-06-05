package com.barakah.gateway.controller;

import com.barakah.gateway.dto.account.*;
import com.barakah.gateway.service.AccountGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Account Management", description = "Account management operations")
public class AccountController {

    private final AccountGatewayService accountService;

    @GetMapping
    @Operation(summary = "List user accounts")
    public ResponseEntity<Page<AccountResponseDto>> listAccounts(Pageable pageable) {
        log.info("Request received: GET /api/v1/accounts - List user accounts (page: {}, size: {})", 
                pageable.getPageNumber(), pageable.getPageSize());
        
        try {
            Page<AccountResponseDto> response = accountService.listUserAccounts(pageable);
            log.info("Successfully retrieved {} accounts (page {} of {})", 
                    response.getNumberOfElements(), 
                    response.getNumber() + 1, 
                    response.getTotalPages());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Failed to list user accounts", ex);
            throw ex;
        }
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Get account by ID")
    public ResponseEntity<AccountResponseDto> getAccount(@PathVariable String accountId) {
        log.info("Request received: GET /api/v1/accounts/{} - Get account by ID", accountId);
        
        if (accountId == null || accountId.trim().isEmpty()) {
            log.warn("Invalid account ID provided: {}", accountId);
            throw new IllegalArgumentException("Account ID cannot be null or empty");
        }
        
        try {
            AccountResponseDto response = accountService.getAccount(accountId, null);
            log.info("Successfully retrieved account: {} ({})", response.getAccountId(), response.getAccountNumber());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Failed to get account with ID: {}", accountId, ex);
            throw ex;
        }
    }

    @PostMapping
    @Operation(summary = "Create account")
    public ResponseEntity<AccountResponseDto> createAccount(@Valid @RequestBody CreateAccountRequestDto request) {
        log.info("Request received: POST /api/v1/accounts - Create account with type: {}", 
                request.getAccountType());
        
        if (request.getAccountType() == null || request.getAccountType().trim().isEmpty()) {
            log.warn("Invalid account type provided: {}", request.getAccountType());
            throw new IllegalArgumentException("Account type cannot be null or empty");
        }
        
        if (request.getInitialDeposit() != null && request.getInitialDeposit().compareTo(java.math.BigDecimal.ZERO) < 0) {
            log.warn("Invalid initial balance provided: {}", request.getInitialDeposit());
            throw new IllegalArgumentException("Initial balance cannot be negative");
        }
        
        try {
            AccountResponseDto response = accountService.createAccount(request);
            log.info("Successfully created account: {} with number: {}", 
                    response.getAccountId(), response.getAccountNumber());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception ex) {
            log.error("Failed to create account with type: {}", request.getAccountType(), ex);
            throw ex;
        }
    }

    @GetMapping("/{accountId}/balance")
    @Operation(summary = "Get account balance")
    public ResponseEntity<BalanceResponseDto> getBalance(@PathVariable String accountId) {
        log.info("Request received: GET /api/v1/accounts/{}/balance - Get account balance", accountId);
        
        if (accountId == null || accountId.trim().isEmpty()) {
            log.warn("Invalid account ID provided for balance check: {}", accountId);
            throw new IllegalArgumentException("Account ID cannot be null or empty");
        }
        
        try {
            BalanceResponseDto response = accountService.getBalance(accountId);
            log.info("Successfully retrieved balance for account: {} - Balance: {}", 
                    accountId, response.getBalance());
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Failed to get balance for account: {}", accountId, ex);
            throw ex;
        }
    }

    @PostMapping("/{accountId}/credit")
    @Operation(summary = "Credit account")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponseDto> creditAccount(
            @PathVariable String accountId,
            @Valid @RequestBody CreditDebitRequestDto request) {
        
        log.info("Request received: POST /api/v1/accounts/{}/credit - Credit account with amount: {}", 
                accountId, request.getAmount());
        
        if (accountId == null || accountId.trim().isEmpty()) {
            log.warn("Invalid account ID provided for credit: {}", accountId);
            throw new IllegalArgumentException("Account ID cannot be null or empty");
        }
        
        if (request.getAmount() == null || request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            log.warn("Invalid credit amount provided: {}", request.getAmount());
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        
        try {
            AccountResponseDto account = accountService.creditAccount(accountId, request);
            log.info("Successfully credited account: {} with amount: {}", accountId, request.getAmount());
            return ResponseEntity.ok(account);
        } catch (Exception ex) {
            log.error("Failed to credit account: {} with amount: {}", accountId, request.getAmount(), ex);
            throw ex;
        }
    }

    @PostMapping("/{accountId}/debit")
    @Operation(summary = "Debit account")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponseDto> debitAccount(
            @PathVariable String accountId,
            @Valid @RequestBody CreditDebitRequestDto request) {
        
        log.info("Request received: POST /api/v1/accounts/{}/debit - Debit account with amount: {}", 
                accountId, request.getAmount());
        
        if (accountId == null || accountId.trim().isEmpty()) {
            log.warn("Invalid account ID provided for debit: {}", accountId);
            throw new IllegalArgumentException("Account ID cannot be null or empty");
        }
        
        if (request.getAmount() == null || request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            log.warn("Invalid debit amount provided: {}", request.getAmount());
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        
        try {
            AccountResponseDto account = accountService.debitAccount(accountId, request);
            log.info("Successfully debited account: {} with amount: {}", accountId, request.getAmount());
            return ResponseEntity.ok(account);
        } catch (Exception ex) {
            log.error("Failed to debit account: {} with amount: {}", accountId, request.getAmount(), ex);
            throw ex;
        }
    }
}