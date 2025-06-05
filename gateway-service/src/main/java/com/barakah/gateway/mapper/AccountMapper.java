package com.barakah.gateway.mapper;

import com.barakah.account.proto.v1.*;
import com.barakah.gateway.dto.account.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class AccountMapper {

    public AccountResponseDto toDto(Account account) {
        return AccountResponseDto.builder()
                .accountId(account.getAccountId())
                .accountNumber(account.getAccountNumber())
                .accountType(account.getAccountType().toString())
                .userId(account.getUserId())
                .accountName(account.getAccountName())
                .balance(new BigDecimal(account.getBalance()))
                .currency("IDR")
                .status(account.getStatus().toString())
                .build();
    }

    public BalanceResponseDto toBalanceDto(GetBalanceResponse response) {
        return BalanceResponseDto.builder()
                .balance(new BigDecimal(response.getBalance()))
                .build();
    }

    public CreateAccountRequest toGrpcCreateRequest(CreateAccountRequestDto dto) {
        CreateAccountRequest.Builder builder = CreateAccountRequest.newBuilder()
                .setAccountType(AccountType.valueOf(dto.getAccountType()))
                .setAccountName(dto.getAccountName())
                .setInitialDeposit(dto.getInitialDeposit().longValue());

        if (dto.getUserId() != null) {
            builder.setUserId(dto.getUserId());
        }

        return builder.build();
    }

    public CreditAccountRequest toCreditRequest(String accountId, CreditDebitRequestDto dto) {
        CreditAccountRequest.Builder builder = CreditAccountRequest.newBuilder()
                .setAccountId(accountId)
                .setAmount(dto.getAmount().longValue());

        if (dto.getDescription() != null) {
            builder.setDescription(dto.getDescription());
        }
        if (dto.getTransactionId() != null) {
            builder.setTransactionId(dto.getTransactionId());
        }
        if (dto.getReference() != null) {
            builder.setReference(dto.getReference());
        }

        return builder.build();
    }

    public DebitAccountRequest toDebitRequest(String accountId, CreditDebitRequestDto dto) {
        DebitAccountRequest.Builder builder = DebitAccountRequest.newBuilder()
                .setAccountId(accountId)
                .setAmount(dto.getAmount().longValue());

        if (dto.getDescription() != null) {
            builder.setDescription(dto.getDescription());
        }
        if (dto.getTransactionId() != null) {
            builder.setTransactionId(dto.getTransactionId());
        }
        if (dto.getReference() != null) {
            builder.setReference(dto.getReference());
        }

        return builder.build();
    }
}