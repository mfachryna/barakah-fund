package com.barakah.account.repository;

import com.barakah.account.entity.Account;
import com.barakah.account.enums.AccountStatus;
import com.barakah.account.enums.AccountType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    Page<Account> findByUserId(String userId, Pageable pageable);

    List<Account> findByUserId(String userId);

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Account a " +
           "WHERE a.userId = :userId AND a.accountType = :accountType AND a.status = :status")
    boolean existsByUserIdAndAccountTypeAndStatus(@Param("userId") String userId, 
                                                  @Param("accountType") AccountType accountType, 
                                                  @Param("status") AccountStatus status);
    
    @Query("SELECT COUNT(a) FROM Account a WHERE a.userId = :userId AND a.accountType = :accountType")
    long countByUserIdAndAccountType(@Param("userId") String userId, @Param("accountType") AccountType accountType);
}