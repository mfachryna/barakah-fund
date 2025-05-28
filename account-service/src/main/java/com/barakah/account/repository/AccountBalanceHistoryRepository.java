package com.barakah.account.repository;

import com.barakah.account.entity.AccountBalanceHistory;
import com.barakah.account.enums.BalanceOperation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AccountBalanceHistoryRepository extends JpaRepository<AccountBalanceHistory, String> {
    
    List<AccountBalanceHistory> findByAccountIdOrderByTimestampDesc(String accountId);
    
    Page<AccountBalanceHistory> findByAccountId(String accountId, Pageable pageable);
    
    Page<AccountBalanceHistory> findByAccountNumber(String accountNumber, Pageable pageable);
    
    List<AccountBalanceHistory> findByTransactionId(String transactionId);
    
    Page<AccountBalanceHistory> findByAccountIdAndOperation(String accountId, BalanceOperation operation, Pageable pageable);
    
    @Query("SELECT abh FROM AccountBalanceHistory abh WHERE " +
           "abh.accountId = :accountId AND " +
           "abh.timestamp BETWEEN :fromDate AND :toDate " +
           "ORDER BY abh.timestamp DESC")
    Page<AccountBalanceHistory> findByAccountIdAndDateRange(
            @Param("accountId") String accountId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);
    
    @Query("SELECT abh FROM AccountBalanceHistory abh WHERE " +
           "(:accountId IS NULL OR abh.accountId = :accountId) AND " +
           "(:operation IS NULL OR abh.operation = :operation) AND " +
           "(:transactionId IS NULL OR abh.transactionId = :transactionId) AND " +
           "(:fromDate IS NULL OR abh.timestamp >= :fromDate) AND " +
           "(:toDate IS NULL OR abh.timestamp <= :toDate)")
    Page<AccountBalanceHistory> findHistoryWithFilters(
            @Param("accountId") String accountId,
            @Param("operation") BalanceOperation operation,
            @Param("transactionId") String transactionId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);
}