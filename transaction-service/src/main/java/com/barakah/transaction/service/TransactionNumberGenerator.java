package com.barakah.transaction.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class TransactionNumberGenerator {

    private static final AtomicLong sequence = new AtomicLong(1);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    public String generateTransactionNumber() {
        String datePart = LocalDateTime.now().format(DATE_FORMAT);
        long sequencePart = sequence.getAndIncrement();
        
        return String.format("TXN%s%06d", datePart, sequencePart);
    }
}