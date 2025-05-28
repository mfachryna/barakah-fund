package com.barakah.transaction.mapper;

import com.barakah.transaction.dto.TransactionResponse;
import com.barakah.transaction.dto.TransferRequest;
import com.barakah.transaction.proto.v1.*;
import com.google.protobuf.Timestamp;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class TransactionProtoMapper {

    public TransferRequest toDtoTransferRequest(
            CreateTransactionRequest protoRequest) {

        TransferRequest dto = new TransferRequest();
        dto.setFromAccountId(protoRequest.getFromAccountNumber());
        dto.setToAccountId(protoRequest.getToAccountNumber());
        dto.setAmount(BigDecimal.valueOf(protoRequest.getAmount()));
        dto.setDescription(protoRequest.getDescription());

        return dto;
    }

    public CreateTransactionResponse toProtoTransferResponse(TransactionResponse response) {
        Transaction protoTransaction = toProtoTransaction(response);

        return CreateTransactionResponse.newBuilder()
                .setTransaction(protoTransaction)
                .setMessage("Transfer processed successfully")
                .build();
    }

    public Transaction toProtoTransaction(TransactionResponse response) {
        Transaction.Builder builder = Transaction.newBuilder()
                .setTransactionId(response.getId())
                .setFromAccountId(response.getFromAccountId())
                .setToAccountId(response.getToAccountId())
                .setAmount(response.getAmount().longValue())
                .setStatus(mapStatus(response.getStatus()));
//                .setCreatedBy(response.getCreatedBy() != null ? response.getCreatedBy() : "");

        if (response.getDescription() != null) {
            builder.setDescription(response.getDescription());
        }

        if (response.getCreatedAt() != null) {
            builder.setCreatedAt(mapLocalDateTimeToTimestamp(response.getCreatedAt()).toString());
        }

        return builder.build();
    }

//    public GetTransactionHistoryResponse toProtoTransactionHistoryResponse(Page<TransactionResponse> transactions) {
//        GetTransactionHistoryResponse.Builder builder = GetTransactionHistoryResponse.newBuilder()
//                .setTotalElements((int) transactions.getTotalElements())
//                .setTotalPages(transactions.getTotalPages())
//                .setCurrentPage(transactions.getNumber())
//                .setPageSize(transactions.getSize());
//
//        transactions.getContent().forEach(transaction ->
//                builder.addTransactions(toProtoTransaction(transaction)));
//
//        return builder.build();
//    }

    private TransactionStatus mapStatus(com.barakah.transaction.enums.TransactionStatus status) {
        return switch (status) {
            case PENDING -> TransactionStatus.PENDING;
            case COMPLETED -> TransactionStatus.COMPLETED;
            case FAILED -> TransactionStatus.FAILED;
            case CANCELLED -> TransactionStatus.CANCELLED;
            case REVERSED -> TransactionStatus.REVERSED;
            default -> TransactionStatus.TRANSACTION_STATUS_UNSPECIFIED;
        };
    }

    private Timestamp mapLocalDateTimeToTimestamp(LocalDateTime dateTime) {
        long seconds = dateTime.toEpochSecond(ZoneOffset.UTC);
        int nanos = dateTime.getNano();

        return Timestamp.newBuilder()
                .setSeconds(seconds)
                .setNanos(nanos)
                .build();
    }
}