package com.barakah.gateway.controller;

import com.barakah.gateway.dto.transaction.*;
import com.barakah.gateway.service.TransactionGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
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
        Page<TransactionResponseDto> transactions = transactionService.listTransactions(filters, search, pageable);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/{transactionId}")
    @Operation(summary = "Get transaction by ID")
    public ResponseEntity<TransactionResponseDto> getTransaction(@PathVariable String transactionId) {
        TransactionResponseDto transaction = transactionService.getTransaction(transactionId, PageRequest.of(0, 10));
        return ResponseEntity.ok(transaction);
    }

    @PostMapping
    @Operation(summary = "Create transaction")
    public ResponseEntity<TransactionResponseDto> createTransaction(@Valid @RequestBody CreateTransactionRequestDto request) {
        TransactionResponseDto transaction = transactionService.createTransaction(request);
        return ResponseEntity.ok(transaction);
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer money between accounts")
    public ResponseEntity<TransactionResponseDto> transfer(@Valid @RequestBody TransferRequestDto request) {
        TransactionResponseDto transaction = transactionService.transfer(request);
        return ResponseEntity.ok(transaction);
    }

//    @GetMapping("/categories")
//    @Operation(summary = "List transaction categories")
//    public ResponseEntity<Page<TransactionCategoryResponseDto>> listCategories(Pageable pageable) {
//        Page<TransactionCategoryResponseDto> categories = transactionService.listCategories(pageable);
//        return ResponseEntity.ok(categories);
//    }

//    @PostMapping("/categories")
//    @Operation(summary = "Create transaction category")
//    public ResponseEntity<TransactionCategoryResponseDto> createCategory(@Valid @RequestBody CreateCategoryRequestDto request) {
//        TransactionCategoryResponseDto category = transactionService.createCategory(request);
//        return ResponseEntity.ok(category);
//    }
}