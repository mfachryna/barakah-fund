package com.barakah.transaction.repository;

import com.barakah.transaction.entity.TransactionCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionCategoryRepository extends JpaRepository<TransactionCategory, String> {
    
    Optional<TransactionCategory> findByName(String name);
    
    boolean existsByName(String name);
    
    List<TransactionCategory> findByIsActiveTrue();
    
    List<TransactionCategory> findByIsSystemTrue();
    
    Page<TransactionCategory> findByIsActive(Boolean isActive, Pageable pageable);
    
    @Query("SELECT tc FROM TransactionCategory tc WHERE " +
           "(:includeInactive = true OR tc.isActive = true) AND " +
           "(:includeSystem = true OR tc.isSystem = false)")
    Page<TransactionCategory> findCategoriesWithFilters(
            @Param("includeInactive") boolean includeInactive,
            @Param("includeSystem") boolean includeSystem,
            Pageable pageable);
    
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.categoryId = :categoryId")
    long countTransactionsByCategoryId(@Param("categoryId") String categoryId);
}