package com.barakah.gateway.mapper;

import com.barakah.gateway.dto.transaction.*;
import com.barakah.transaction.proto.v1.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class TransactionMapper {

    public TransactionResponseDto toDto(Transaction transaction) {
        return TransactionResponseDto.builder()
                .transactionId(transaction.getTransactionId())
                .transactionType(transaction.getType().toString())
                .fromAccountId(transaction.getFromAccountId())
                .fromAccountNumber(transaction.getFromAccountNumber())
                .toAccountId(transaction.getToAccountId())
                .toAccountNumber(transaction.getToAccountNumber())
                .amount(new BigDecimal(transaction.getAmount()))
                .currency(transaction.getCurrency())
                .description(transaction.getDescription())
                .status(transaction.getStatus().name())
                .categoryId(transaction.getCategoryId())
//                .categoryName(transaction.getCategory().getName())
//                .balanceBeforeFrom(transaction.getBalanceBefore() ? null :
//                    new BigDecimal(transaction.getBalanceBeforeFrom()))
//                .balanceAfterFrom(transaction.getBalanceAfterFrom().isEmpty() ? null :
//                    new BigDecimal(transaction.getBalanceAfterFrom()))
//                .balanceBeforeTo(transaction.getBalanceBeforeTo().isEmpty() ? null :
//                    new BigDecimal(transaction.getBalanceBeforeTo()))
//                .balanceAfterTo(transaction.getBalanceAfterTo().isEmpty() ? null :
//                    new BigDecimal(transaction.getBalanceAfterTo()))
//                .createdAt(transaction.getCreatedAt().isEmpty() ? null :
//                    LocalDateTime.parse(transaction.getCreatedAt(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
//                .processedAt(transaction.getProcessedAt().isEmpty() ? null :
//                    LocalDateTime.parse(transaction.getProcessedAt(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .reference(transaction.getReferenceNumber())
                .notes(transaction.getNotes())
                .build();
    }

    public TransactionCategoryResponseDto toCategoryDto(TransactionCategory category) {
        return TransactionCategoryResponseDto.builder()
                .categoryId(category.getCategoryId())
                .name(category.getName())
                .description(category.getDescription())
                .color(category.getColor())
                .icon(category.getIcon())
                .isActive(category.getIsActive())
                .createdAt(category.getCreatedAt().isEmpty() ? null : 
                    LocalDateTime.parse(category.getCreatedAt(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .updatedAt(category.getUpdatedAt().isEmpty() ? null : 
                    LocalDateTime.parse(category.getUpdatedAt(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }

    public CreateTransactionRequest toGrpcCreateRequest(CreateTransactionRequestDto dto) {
        CreateTransactionRequest.Builder builder = CreateTransactionRequest.newBuilder()
                .setType(TransactionType.valueOf(dto.getTransactionType()))
                .setFromAccountNumber(dto.getAccountId())
                .setAmount(dto.getAmount().longValue());

        if (dto.getDescription() != null) {
            builder.setDescription(dto.getDescription());
        }
        if (dto.getCategoryId() != null) {
            builder.setCategoryId(dto.getCategoryId());
        }
        if (dto.getReference() != null) {
            builder.setExternalReference(dto.getReference());
        }
        if (dto.getNotes() != null) {
            builder.setNotes(dto.getNotes());
        }

        return builder.build();
    }

    public CreateTransactionRequest toGrpcTransferRequest(TransferRequestDto dto) {
        CreateTransactionRequest.Builder builder = CreateTransactionRequest.newBuilder()
                .setFromAccountNumber(dto.getFromAccountNumber())
                .setToAccountNumber(dto.getToAccountNumber())
                .setAmount(dto.getAmount().longValue());

        if (dto.getDescription() != null) {
            builder.setDescription(dto.getDescription());
        }
        if (dto.getCategoryId() != null) {
            builder.setCategoryId(dto.getCategoryId());
        }
        if (dto.getReference() != null) {
            builder.setExternalReference(dto.getReference());
        }
        if (dto.getNotes() != null) {
            builder.setNotes(dto.getNotes());
        }

        return builder.build();
    }

    public CreateCategoryRequest toGrpcCategoryRequest(CreateCategoryRequestDto dto) {
        CreateCategoryRequest.Builder builder = CreateCategoryRequest.newBuilder()
                .setName(dto.getName());

        if (dto.getDescription() != null) {
            builder.setDescription(dto.getDescription());
        }
        if (dto.getColor() != null) {
            builder.setColor(dto.getColor());
        }
        if (dto.getIcon() != null) {
            builder.setIcon(dto.getIcon());
        }

        return builder.build();
    }
}