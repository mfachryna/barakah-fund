package com.barakah.gateway.controller;

import com.barakah.gateway.dto.account.*;
import com.barakah.gateway.service.AccountGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
        Page<AccountResponseDto> accounts = accountService.listUserAccounts(pageable);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{accountId}")
    @Operation(summary = "Get account by ID")
    public ResponseEntity<AccountResponseDto> getAccount(@PathVariable String accountId) {
        AccountResponseDto account = accountService.getAccount(accountId, PageRequest.of(0, 10));
        return ResponseEntity.ok(account);
    }

    @PostMapping
    @Operation(summary = "Create account")
    public ResponseEntity<AccountResponseDto> createAccount(@Valid @RequestBody CreateAccountRequestDto request) {
        AccountResponseDto account = accountService.createAccount(request);
        return ResponseEntity.ok(account);
    }

    @GetMapping("/{accountId}/balance")
    @Operation(summary = "Get account balance")
    public ResponseEntity<BalanceResponseDto> getBalance(@PathVariable String accountId) {
        BalanceResponseDto balance = accountService.getBalance(accountId);
        return ResponseEntity.ok(balance);
    }

    @PostMapping("/{accountId}/credit")
    @Operation(summary = "Credit account")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponseDto> creditAccount(
            @PathVariable String accountId,
            @Valid @RequestBody CreditDebitRequestDto request) {
        AccountResponseDto account = accountService.creditAccount(accountId, request);
        return ResponseEntity.ok(account);
    }

    @PostMapping("/{accountId}/debit")
    @Operation(summary = "Debit account")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountResponseDto> debitAccount(
            @PathVariable String accountId,
            @Valid @RequestBody CreditDebitRequestDto request) {
        AccountResponseDto account = accountService.debitAccount(accountId, request);
        return ResponseEntity.ok(account);
    }
}