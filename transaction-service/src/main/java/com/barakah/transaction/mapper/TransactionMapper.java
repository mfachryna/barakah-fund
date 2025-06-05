package com.barakah.transaction.mapper;

import com.barakah.transaction.entity.Transaction;
import com.barakah.transaction.entity.TransactionCategory;
import com.barakah.transaction.entity.TransactionLog;
import com.barakah.transaction.proto.v1.*;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public class TransactionMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public com.barakah.transaction.proto.v1.Transaction toProto(Transaction entity) {
        if (entity == null) {
            return null;
        }

        var builder = com.barakah.transaction.proto.v1.Transaction.newBuilder()
                .setTransactionId(entity.getTransactionId())
                .setReferenceNumber(entity.getReferenceNumber())
                .setType(mapTransactionType(entity.getType()))
                .setStatus(mapTransactionStatus(entity.getStatus()))
                .setDirection(mapTransactionDirection(entity.getDirection()))
                .setAmount(convertToLong(entity.getAmount()))
                .setCurrency(entity.getCurrency())
                .setCreatedAt(entity.getCreatedAt().format(FORMATTER))
                .setUpdatedAt(entity.getUpdatedAt().format(FORMATTER));

        if (entity.getTransferType() != null) {
            builder.setTransferType(mapTransferType(entity.getTransferType()));
        }

        if (entity.getFromAccountId() != null) {
            builder.setFromAccountId(entity.getFromAccountId());
        }

        if (entity.getFromAccountNumber() != null) {
            builder.setFromAccountNumber(entity.getFromAccountNumber());
        }

        if (entity.getToAccountId() != null) {
            builder.setToAccountId(entity.getToAccountId());
        }

        if (entity.getToAccountNumber() != null) {
            builder.setToAccountNumber(entity.getToAccountNumber());
        }

        if (entity.getDescription() != null) {
            builder.setDescription(entity.getDescription());
        }

        if (entity.getNotes() != null) {
            builder.setNotes(entity.getNotes());
        }

        if (entity.getCategoryId() != null) {
            builder.setCategoryId(entity.getCategoryId());
        }

        if (entity.getCategory() != null) {
            builder.setCategory(toProto(entity.getCategory()));
        }

        if (entity.getBalanceBefore() != null) {
            builder.setBalanceBefore(convertToLong(entity.getBalanceBefore()));
        }

        if (entity.getBalanceAfter() != null) {
            builder.setBalanceAfter(convertToLong(entity.getBalanceAfter()));
        }

        if (entity.getCreatedBy() != null) {
            builder.setCreatedBy(entity.getCreatedBy());
        }

        if (entity.getUpdatedBy() != null) {
            builder.setUpdatedBy(entity.getUpdatedBy());
        }

        if (entity.getExternalReference() != null) {
            builder.setExternalReference(entity.getExternalReference());
        }

        if (entity.getExternalProvider() != null) {
            builder.setExternalProvider(entity.getExternalProvider());
        }

        return builder.build();
    }

    public List<com.barakah.transaction.proto.v1.Transaction> toProtoList(List<Transaction> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toProto).collect(Collectors.toList());
    }

    public com.barakah.transaction.proto.v1.TransactionCategory toProto(TransactionCategory entity) {
        if (entity == null) {
            return null;
        }

        var builder = com.barakah.transaction.proto.v1.TransactionCategory.newBuilder()
                .setCategoryId(entity.getCategoryId())
                .setName(entity.getName())
                .setIsActive(entity.getIsActive())
                .setIsSystem(entity.getIsSystem())
                .setCreatedAt(entity.getCreatedAt().format(FORMATTER))
                .setUpdatedAt(entity.getUpdatedAt().format(FORMATTER));

        if (entity.getDescription() != null) {
            builder.setDescription(entity.getDescription());
        }

        if (entity.getIcon() != null) {
            builder.setIcon(entity.getIcon());
        }

        if (entity.getColor() != null) {
            builder.setColor(entity.getColor());
        }

        if (entity.getCreatedBy() != null) {
            builder.setCreatedBy(entity.getCreatedBy());
        }

        return builder.build();
    }

    public List<com.barakah.transaction.proto.v1.TransactionCategory> toCategoryProtoList(List<TransactionCategory> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toProto).collect(Collectors.toList());
    }

    public com.barakah.transaction.proto.v1.TransactionLog toProto(TransactionLog entity) {
        if (entity == null) {
            return null;
        }

        var builder = com.barakah.transaction.proto.v1.TransactionLog.newBuilder()
                .setLogId(entity.getLogId())
                .setTransactionId(entity.getTransactionId())
                .setAccountId(entity.getAccountId())
                .setAccountNumber(entity.getAccountNumber())
                .setDirection(mapTransactionDirection(entity.getDirection()))
                .setAmount(convertToLong(entity.getAmount()))
                .setBalanceBefore(convertToLong(entity.getBalanceBefore()))
                .setBalanceAfter(convertToLong(entity.getBalanceAfter()))
                .setTimestamp(entity.getTimestamp().format(FORMATTER));

        if (entity.getNotes() != null) {
            builder.setNotes(entity.getNotes());
        }

        return builder.build();
    }

    public List<com.barakah.transaction.proto.v1.TransactionLog> toLogProtoList(List<TransactionLog> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream().map(this::toProto).collect(Collectors.toList());
    }

    public com.barakah.transaction.proto.v1.TransactionType mapTransactionType(com.barakah.transaction.enums.TransactionType type) {
        if (type == null) {
            return com.barakah.transaction.proto.v1.TransactionType.TRANSACTION_TYPE_UNSPECIFIED;
        }

        return switch (type) {
            case TRANSFER ->
                com.barakah.transaction.proto.v1.TransactionType.TRANSFER;
            case DEPOSIT ->
                com.barakah.transaction.proto.v1.TransactionType.DEPOSIT;
            case WITHDRAWAL ->
                com.barakah.transaction.proto.v1.TransactionType.WITHDRAWAL;
            case PAYMENT ->
                com.barakah.transaction.proto.v1.TransactionType.PAYMENT;
            case REFUND ->
                com.barakah.transaction.proto.v1.TransactionType.REFUND;
            case FEE ->
                com.barakah.transaction.proto.v1.TransactionType.FEE;
            case INTEREST ->
                com.barakah.transaction.proto.v1.TransactionType.INTEREST;
        };
    }

    public com.barakah.transaction.enums.TransactionType mapTransactionType(com.barakah.transaction.proto.v1.TransactionType type) {
        if (type == null) {
            return null;
        }

        return switch (type) {
            case TRANSFER ->
                com.barakah.transaction.enums.TransactionType.TRANSFER;
            case DEPOSIT ->
                com.barakah.transaction.enums.TransactionType.DEPOSIT;
            case WITHDRAWAL ->
                com.barakah.transaction.enums.TransactionType.WITHDRAWAL;
            case PAYMENT ->
                com.barakah.transaction.enums.TransactionType.PAYMENT;
            case REFUND ->
                com.barakah.transaction.enums.TransactionType.REFUND;
            case FEE ->
                com.barakah.transaction.enums.TransactionType.FEE;
            case INTEREST ->
                com.barakah.transaction.enums.TransactionType.INTEREST;
            case TRANSACTION_TYPE_UNSPECIFIED, UNRECOGNIZED ->
                null;
        };
    }

    public com.barakah.transaction.proto.v1.TransactionStatus mapTransactionStatus(com.barakah.transaction.enums.TransactionStatus status) {
        if (status == null) {
            return com.barakah.transaction.proto.v1.TransactionStatus.TRANSACTION_STATUS_UNSPECIFIED;
        }

        return switch (status) {
            case PENDING ->
                com.barakah.transaction.proto.v1.TransactionStatus.PENDING;
            case PROCESSING ->
                com.barakah.transaction.proto.v1.TransactionStatus.PROCESSING;
            case COMPLETED ->
                com.barakah.transaction.proto.v1.TransactionStatus.COMPLETED;
            case FAILED ->
                com.barakah.transaction.proto.v1.TransactionStatus.FAILED;
            case CANCELLED ->
                com.barakah.transaction.proto.v1.TransactionStatus.CANCELLED;
            case REVERSED ->
                com.barakah.transaction.proto.v1.TransactionStatus.REVERSED;
        };
    }

    public com.barakah.transaction.enums.TransactionStatus mapTransactionStatus(com.barakah.transaction.proto.v1.TransactionStatus status) {
        if (status == null) {
            return null;
        }

        return switch (status) {
            case PENDING ->
                com.barakah.transaction.enums.TransactionStatus.PENDING;
            case PROCESSING ->
                com.barakah.transaction.enums.TransactionStatus.PROCESSING;
            case COMPLETED ->
                com.barakah.transaction.enums.TransactionStatus.COMPLETED;
            case FAILED ->
                com.barakah.transaction.enums.TransactionStatus.FAILED;
            case CANCELLED ->
                com.barakah.transaction.enums.TransactionStatus.CANCELLED;
            case REVERSED ->
                com.barakah.transaction.enums.TransactionStatus.REVERSED;
            case TRANSACTION_STATUS_UNSPECIFIED, UNRECOGNIZED ->
                null;
        };
    }

    public com.barakah.transaction.proto.v1.TransactionDirection mapTransactionDirection(com.barakah.transaction.enums.TransactionDirection direction) {
        if (direction == null) {
            return com.barakah.transaction.proto.v1.TransactionDirection.TRANSACTION_DIRECTION_UNSPECIFIED;
        }

        return switch (direction) {
            case DEBIT ->
                com.barakah.transaction.proto.v1.TransactionDirection.DEBIT;
            case CREDIT ->
                com.barakah.transaction.proto.v1.TransactionDirection.CREDIT;
        };
    }

    public com.barakah.transaction.enums.TransactionDirection mapTransactionDirection(com.barakah.transaction.proto.v1.TransactionDirection direction) {
        if (direction == null) {
            return null;
        }

        return switch (direction) {
            case DEBIT ->
                com.barakah.transaction.enums.TransactionDirection.DEBIT;
            case CREDIT ->
                com.barakah.transaction.enums.TransactionDirection.CREDIT;
            case TRANSACTION_DIRECTION_UNSPECIFIED, UNRECOGNIZED ->
                null;
        };
    }

    public com.barakah.transaction.proto.v1.TransferType mapTransferType(com.barakah.transaction.enums.TransferType transferType) {
        if (transferType == null) {
            return com.barakah.transaction.proto.v1.TransferType.TRANSFER_TYPE_UNSPECIFIED;
        }

        return switch (transferType) {
            case INTERNAL ->
                com.barakah.transaction.proto.v1.TransferType.INTERNAL;
            case EXTERNAL ->
                com.barakah.transaction.proto.v1.TransferType.EXTERNAL;
        };
    }

    public com.barakah.transaction.enums.TransferType mapTransferType(com.barakah.transaction.proto.v1.TransferType transferType) {
        if (transferType == null) {
            return null;
        }

        return switch (transferType) {
            case INTERNAL ->
                com.barakah.transaction.enums.TransferType.INTERNAL;
            case EXTERNAL ->
                com.barakah.transaction.enums.TransferType.EXTERNAL;
            case TRANSFER_TYPE_UNSPECIFIED, UNRECOGNIZED ->
                null;
        };
    }

    private long convertToLong(BigDecimal amount) {
        if (amount == null) {
            return 0;
        }

        return amount.longValue();
    }

    private BigDecimal convertToBigDecimal(long amount) {

        return BigDecimal.valueOf(amount);
    }

    public List<com.barakah.transaction.enums.TransactionType> mapTransactionTypes(List<com.barakah.transaction.proto.v1.TransactionType> protoTypes) {
        if (protoTypes == null || protoTypes.isEmpty()) {
            return List.of();
        }
        return protoTypes.stream()
                .map(this::mapTransactionType)
                .filter(type -> type != null)
                .collect(Collectors.toList());
    }

    public List<com.barakah.transaction.enums.TransactionStatus> mapTransactionStatuses(List<com.barakah.transaction.proto.v1.TransactionStatus> protoStatuses) {
        if (protoStatuses == null || protoStatuses.isEmpty()) {
            return List.of();
        }
        return protoStatuses.stream()
                .map(this::mapTransactionStatus)
                .filter(status -> status != null)
                .collect(Collectors.toList());
    }
}
