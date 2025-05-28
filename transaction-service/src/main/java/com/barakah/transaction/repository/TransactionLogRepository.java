package com.barakah.transaction.repository;

import com.barakah.transaction.entity.TransactionLog;
import com.barakah.transaction.enums.TransactionDirection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionLogRepository extends JpaRepository<TransactionLog, String> {
    
    List<TransactionLog> findByTransactionIdOrderByTimestampDesc(String transactionId);
    
    Page<TransactionLog> findByTransactionId(String transactionId, Pageable pageable);
    
    Page<TransactionLog> findByAccountId(String accountId, Pageable pageable);
    
    Page<TransactionLog> findByAccountNumber(String accountNumber, Pageable pageable);
    
    List<TransactionLog> findByAccountIdAndDirection(String accountId, TransactionDirection direction);
    
    @Query("SELECT tl FROM TransactionLog tl WHERE " +
           "(:transactionId IS NULL OR tl.transactionId = :transactionId) AND " +
           "(:accountId IS NULL OR tl.accountId = :accountId) AND " +
           "(:direction IS NULL OR tl.direction = :direction) AND " +
           "(:fromDate IS NULL OR tl.timestamp >= :fromDate) AND " +
           "(:toDate IS NULL OR tl.timestamp <= :toDate)")
    Page<TransactionLog> findLogsWithFilters(
            @Param("transactionId") String transactionId,
            @Param("accountId") String accountId,
            @Param("direction") TransactionDirection direction,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);
}