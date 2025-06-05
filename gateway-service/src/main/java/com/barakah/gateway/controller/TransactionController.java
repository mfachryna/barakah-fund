package com.barakah.gateway.controller;

import com.barakah.gateway.dto.transaction.*;
import com.barakah.gateway.service.TransactionGatewayService;
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

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction Management", description = "Transaction operations")
public class TransactionController {

    private final TransactionGatewayService transactionService;

    @GetMapping
    @Operation(summary = "List transactions")
    public ResponseEntity<Page<TransactionResponseDto>> listTransactions(
            @RequestParam(required = false) Map<String, String> filters,
            @RequestParam(required = false) String search,
            Pageable pageable) {

        log.info("Request received: GET /api/v1/transactions - List transactions (page: {}, size: {}, search: {}, filters: {})",
                pageable.getPageNumber(), pageable.getPageSize(), search, filters);

        try {
            Page<TransactionResponseDto> transactions = transactionService.listTransactions(pageable);
            log.info("Successfully retrieved {} transactions (page {} of {})",
                    transactions.getNumberOfElements(),
                    transactions.getNumber() + 1,
                    transactions.getTotalPages());
            return ResponseEntity.ok(transactions);
        } catch (Exception ex) {
            log.error("Failed to list transactions", ex);
            throw ex;
        }
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<TransactionResponseDto> getTransaction(@PathVariable String transactionId) {
        log.info("Request received: GET /api/v1/transactions/{} - Get transaction by ID", transactionId);

        if (transactionId == null || transactionId.trim().isEmpty()) {
            log.warn("Invalid transaction ID provided: {}", transactionId);
            throw new IllegalArgumentException("Transaction ID cannot be null or empty");
        }

        try {
            TransactionResponseDto transaction = transactionService.getTransaction(transactionId);
            log.info("Successfully retrieved transaction: {} with amount: {}",
                    transaction.getTransactionId(), transaction.getAmount());
            return ResponseEntity.ok(transaction);
        } catch (Exception ex) {
            log.error("Failed to get transaction with ID: {}", transactionId, ex);
            throw ex;
        }
    }

    @PostMapping
    @Operation(summary = "Create transaction")
    public ResponseEntity<TransactionResponseDto> createTransaction(
            @Valid @RequestBody CreateTransactionRequestDto request) {

        log.info("Request received: POST /api/v1/transactions - Create transaction with amount: {} from account: {} to account: {}",
                request.getAmount(), request.getFromAccountNumber(), request.getToAccountNumber());

        if (request.getAmount() == null || request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            log.warn("Invalid transaction amount provided: {}", request.getAmount());
            throw new IllegalArgumentException("Transaction amount must be positive");
        }

        if (request.getFromAccountNumber() == null || request.getFromAccountNumber().trim().isEmpty()) {
            log.warn("Invalid from account ID provided: {}", request.getFromAccountNumber());
            throw new IllegalArgumentException("From account ID cannot be null or empty");
        }

        if (request.getToAccountNumber() == null || request.getToAccountNumber().trim().isEmpty()) {
            log.warn("Invalid to account ID provided: {}", request.getToAccountNumber());
            throw new IllegalArgumentException("To account ID cannot be null or empty");
        }

        if (request.getFromAccountNumber().equals(request.getToAccountNumber())) {
            log.warn("Same account provided for from and to: {}", request.getFromAccountNumber());
            throw new IllegalArgumentException("From and to account cannot be the same");
        }

        try {
            TransactionResponseDto transaction = transactionService.createTransaction(request);
            log.info("Successfully created transaction: {} with amount: {}",
                    transaction.getTransactionId(), transaction.getAmount());
            return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
        } catch (Exception ex) {
            log.error("Failed to create transaction with amount: {} from account: {} to account: {}",
                    request.getAmount(), request.getFromAccountNumber(), request.getToAccountNumber(), ex);
            throw ex;
        }
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get transactions by account")
    public ResponseEntity<Page<TransactionResponseDto>> getTransactionsByAccount(
            @PathVariable String accountId,
            Pageable pageable) {

        log.info("Request received: GET /api/v1/transactions/account/{} - Get transactions by account (page: {}, size: {})",
                accountId, pageable.getPageNumber(), pageable.getPageSize());

        if (accountId == null || accountId.trim().isEmpty()) {
            log.warn("Invalid account ID provided: {}", accountId);
            throw new IllegalArgumentException("Account ID cannot be null or empty");
        }

        try {
            Page<TransactionResponseDto> transactions = transactionService.getTransactionsByAccount(accountId, pageable);
            log.info("Successfully retrieved {} transactions for account: {} (page {} of {})",
                    transactions.getNumberOfElements(),
                    accountId,
                    transactions.getNumber() + 1,
                    transactions.getTotalPages());
            return ResponseEntity.ok(transactions);
        } catch (Exception ex) {
            log.error("Failed to get transactions for account: {}", accountId, ex);
            throw ex;
        }
    }

    @GetMapping("/categories")
    @Operation(summary = "List transaction categories")
    public ResponseEntity<Page<TransactionCategoryResponseDto>> listCategories(
            @RequestParam(value = "include_system", required = false, defaultValue = "false") boolean includeSystem,
            Pageable pageable) {

        log.info("Request received: GET /api/v1/transactions/categories - List categories (includeSystem: {})",
                includeSystem);

        try {
            Page<TransactionCategoryResponseDto> categories = transactionService.listCategories(includeSystem,pageable);
            log.info("Successfully retrieved {} categories", categories.getSize());
            return ResponseEntity.ok(categories);
        } catch (Exception ex) {
            log.error("Failed to list categories", ex);
            throw ex;
        }
    }

    @GetMapping("/categories/{categoryId}")
    @Operation(summary = "Get transaction category by ID")
    public ResponseEntity<TransactionCategoryResponseDto> getCategory(@PathVariable String categoryId) {
        log.info("Request received: GET /api/v1/transactions/categories/{} - Get category by ID", categoryId);

        if (categoryId == null || categoryId.trim().isEmpty()) {
            log.warn("Invalid category ID provided: {}", categoryId);
            throw new IllegalArgumentException("Category ID cannot be null or empty");
        }

        try {
            TransactionCategoryResponseDto category = transactionService.getCategory(categoryId);
            log.info("Successfully retrieved category: {} ({})", category.getCategoryId(), category.getName());
            return ResponseEntity.ok(category);
        } catch (Exception ex) {
            log.error("Failed to get category with ID: {}", categoryId, ex);
            throw ex;
        }
    }
}