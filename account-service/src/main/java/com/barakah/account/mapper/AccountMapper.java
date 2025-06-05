package com.barakah.account.mapper;

import com.barakah.account.dto.AccountResponse;
import com.barakah.account.entity.Account;
import com.barakah.account.proto.v1.*;
import com.barakah.common.proto.v1.AuditInfo;
import com.barakah.common.proto.v1.Status;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    AccountResponse toResponse(Account account);

    @Mapping(target = "accountId", source = "id")
    @Mapping(target = "balance", source = "balance", qualifiedByName = "bigDecimalToLong")
    @Mapping(target = "accountType", source = "accountType", qualifiedByName = "accountTypeToProto")
    @Mapping(target = "status", source = "status", qualifiedByName = "accountStatusToProtoStatus")
    @Mapping(target = "auditInfo", source = ".", qualifiedByName = "toAuditInfo")
    com.barakah.account.proto.v1.Account toProto(Account account);

    @Named("bigDecimalToLong")
    default long bigDecimalToLong(BigDecimal value) {
        return value != null ? value.longValue() : 0L;
    }

    @Named("longToBigDecimal")
    default BigDecimal longToBigDecimal(long value) {
        return BigDecimal.valueOf(value);
    }

    @Named("toAuditInfo")
    default AuditInfo toAuditInfo(Account account) {
        return AuditInfo.newBuilder()
                .setCreatedAt(account.getCreatedAt() != null
                        ? account.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "")
                .setUpdatedAt(account.getUpdatedAt() != null
                        ? account.getUpdatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "")
                .build();
    }

    @Named("accountTypeToProto")
    default com.barakah.account.proto.v1.AccountType accountTypeToProto(com.barakah.account.enums.AccountType accountType) {
        if (accountType == null) {
            return com.barakah.account.proto.v1.AccountType.SAVINGS;
        }

        return switch (accountType) {
            case SAVINGS ->
                com.barakah.account.proto.v1.AccountType.SAVINGS;
            case SPENDING ->
                com.barakah.account.proto.v1.AccountType.SPENDING;
        };
    }

    @Named("protoToAccountType")
    default com.barakah.account.enums.AccountType protoToAccountType(com.barakah.account.proto.v1.AccountType accountType) {
        if (accountType == null) {
            return null;
        }

        return switch (accountType) {
            case SAVINGS ->
                com.barakah.account.enums.AccountType.SAVINGS;
            case SPENDING ->
                com.barakah.account.enums.AccountType.SPENDING;
            default ->
                null;
        };
    }

    @Named("accountStatusToProtoStatus")
    default Status accountStatusToProtoStatus(com.barakah.account.enums.AccountStatus status) {
        if (status == null) {
            return Status.ACTIVE;
        }

        return switch (status) {
            case ACTIVE ->
                Status.ACTIVE;
            case FROZEN ->
                Status.SUSPENDED;
            case CLOSED ->
                Status.INACTIVE;
        };
    }
}
