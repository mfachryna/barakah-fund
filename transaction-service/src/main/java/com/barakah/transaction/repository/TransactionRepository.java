package com.barakah.transaction.repository;

import com.barakah.transaction.entity.Transaction;
import com.barakah.transaction.enums.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String>,
        JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    boolean existsByReferenceNumber(String referenceNumber);

    @Query(value = "SELECT t FROM Transaction t LEFT JOIN FETCH t.category",
            countQuery = "SELECT COUNT(t) FROM Transaction t")
    Page<Transaction> findAllWithCategoriesAndSpec(Specification<Transaction> spec, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.fromAccountId = :accountId OR t.toAccountId = :accountId")
    Page<Transaction> findByAccountId(@Param("accountId") String accountId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.fromAccountNumber = :accountNumber OR t.toAccountNumber = :accountNumber")
    Page<Transaction> findByAccountNumber(@Param("accountNumber") String accountNumber, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.fromAccountId IN :accountIds OR t.toAccountId IN :accountIds")
    Page<Transaction> findByUserAccountIds(@Param("accountIds") List<String> accountIds, Pageable pageable);

    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);

    Page<Transaction> findByStatusIn(List<TransactionStatus> statuses, Pageable pageable);

    Page<Transaction> findByType(TransactionType type, Pageable pageable);

    Page<Transaction> findByTypeIn(List<TransactionType> types, Pageable pageable);

    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.category WHERE t.transactionId = :id")
    Optional<Transaction> findByIdWithCategory(@Param("id") String transactionId);

    @Query("SELECT t FROM Transaction t WHERE "
            + "(t.direction = :direction) AND "
            + "(t.fromAccountId = :accountId OR t.toAccountId = :accountId)")
    Page<Transaction> findByAccountIdAndDirection(@Param("accountId") String accountId,
            @Param("direction") TransactionDirection direction,
            Pageable pageable);

    Page<Transaction> findByTransferType(TransferType transferType, Pageable pageable);

    Page<Transaction> findByCategoryId(String categoryId, Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.createdAt BETWEEN :fromDate AND :toDate")
    Page<Transaction> findByDateRange(@Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE "
            + "(:accountId IS NULL OR t.fromAccountId = :accountId OR t.toAccountId = :accountId) AND "
            + "(:types IS NULL OR t.type IN :types) AND "
            + "(:statuses IS NULL OR t.status IN :statuses) AND "
            + "(:direction IS NULL OR t.direction = :direction) AND "
            + "(:transferType IS NULL OR t.transferType = :transferType) AND "
            + "(:categoryId IS NULL OR t.categoryId = :categoryId) AND "
            + "(:fromDate IS NULL OR t.createdAt >= :fromDate) AND "
            + "(:toDate IS NULL OR t.createdAt <= :toDate)")
    Page<Transaction> findTransactionsWithFilters(
            @Param("accountId") String accountId,
            @Param("types") List<TransactionType> types,
            @Param("statuses") List<TransactionStatus> statuses,
            @Param("direction") TransactionDirection direction,
            @Param("transferType") TransferType transferType,
            @Param("categoryId") String categoryId,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    long countByStatus(TransactionStatus status);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.fromAccountId = :accountId OR t.toAccountId = :accountId")
    long countByAccountId(@Param("accountId") String accountId);

    @Query("SELECT t FROM Transaction t WHERE t.status = 'PENDING' AND t.createdAt < :before")
    List<Transaction> findPendingTransactionsOlderThan(@Param("before") LocalDateTime before);

    Page<Transaction> findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(
            String fromAccountId, String toAccountId, Pageable pageable);

    Page<Transaction> findByStatusOrderByCreatedAtDesc(TransactionStatus status, Pageable pageable);

    List<Transaction> findByFromAccountIdAndCreatedAtBetween(
            String accountId, LocalDateTime startDate, LocalDateTime endDate);

    List<Transaction> findByToAccountIdAndCreatedAtBetween(
            String accountId, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.fromAccountId = :accountId "
            + "AND t.createdAt >= :startTime AND t.status = 'COMPLETED'")
    Long countRecentTransactions(@Param("accountId") String accountId,
            @Param("startTime") LocalDateTime startTime);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.fromAccountId = :accountId "
            + "AND t.createdAt >= :startTime AND t.status = 'COMPLETED'")
    BigDecimal sumRecentTransactionAmounts(@Param("accountId") String accountId,
            @Param("startTime") LocalDateTime startTime);

    @Query("SELECT t FROM Transaction t WHERE t.status = 'PENDING' "
            + "AND t.createdAt < :cutoffTime")
    List<Transaction> findStaleTransactions(@Param("cutoffTime") LocalDateTime cutoffTime);

    @Query("SELECT t FROM Transaction t WHERE t.type = :type "
            + "AND t.createdAt BETWEEN :startDate AND :endDate "
            + "ORDER BY t.amount DESC")
    List<Transaction> findTransactionsByTypeAndDateRange(
            @Param("type") TransactionType type,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t FROM Transaction t WHERE "
            + "(t.fromAccountId = :accountId OR t.toAccountId = :accountId) "
            + "AND t.amount >= :minAmount "
            + "AND t.status = 'COMPLETED' "
            + "ORDER BY t.createdAt DESC")
    Page<Transaction> findLargeTransactionsByAccount(
            @Param("accountId") String accountId,
            @Param("minAmount") BigDecimal minAmount,
            Pageable pageable);

}
