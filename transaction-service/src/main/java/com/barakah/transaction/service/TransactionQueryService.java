package com.barakah.transaction.service;

import com.barakah.transaction.entity.Transaction;
import com.barakah.transaction.enums.*;
import com.barakah.transaction.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionQueryService {

    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public Page<Transaction> listTransactions(String userId, Map<String, String> filters, 
                                              String search, Pageable pageable) {
        
        Specification<Transaction> spec = buildTransactionSpecification(userId, filters, search);
        return transactionRepository.findAllWithCategoriesAndSpec(spec, pageable);
    }

    private Specification<Transaction> buildTransactionSpecification(String userId, 
                                                                    Map<String, String> filters, 
                                                                    String search) {
        return Specification.<Transaction>where(null)
                .and(hasUserAccess(userId))
                .and(buildFiltersSpecification(filters))
                .and(buildSearchSpecification(search));
    }

    private Specification<Transaction> hasUserAccess(String userId) {
        return (root, query, criteriaBuilder) -> {
            return criteriaBuilder.or(
                criteriaBuilder.equal(root.get("createdBy"), userId),
                criteriaBuilder.exists(
                    query.subquery(String.class)
                        .select(root.get("fromAccountId"))
                        .where(criteriaBuilder.equal(root.get("fromAccountId"), userId))
                )
            );
        };
    }

    private Specification<Transaction> buildFiltersSpecification(Map<String, String> filters) {
        if (filters == null || filters.isEmpty()) {
            return null;
        }

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            for (Map.Entry<String, String> entry : filters.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                
                if (value == null || value.trim().isEmpty()) {
                    continue;
                }

                Predicate predicate = createFilterPredicate(root, criteriaBuilder, key, value);
                if (predicate != null) {
                    predicates.add(predicate);
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Predicate createFilterPredicate(Root<Transaction> root, CriteriaBuilder cb, 
                                           String key, String value) {
        try {
            return switch (key.toLowerCase()) {
                case "type", "transaction_type" -> createEnumPredicate(root, cb, "type", value, TransactionType.class);
                case "status", "transaction_status" -> createEnumPredicate(root, cb, "status", value, TransactionStatus.class);
                case "direction", "transaction_direction" -> createEnumPredicate(root, cb, "direction", value, TransactionDirection.class);
                case "transfer_type", "transfertype" -> createEnumPredicate(root, cb, "transferType", value, TransferType.class);
                
                
                case "from_account_id", "fromaccountid" -> cb.equal(root.get("fromAccountId"), value);
                case "from_account_number", "fromaccountnumber" -> cb.equal(root.get("fromAccountNumber"), value);
                case "to_account_id", "toaccountid" -> cb.equal(root.get("toAccountId"), value);
                case "to_account_number", "toaccountnumber" -> cb.equal(root.get("toAccountNumber"), value);
                case "account_id", "accountid" -> cb.or(
                    cb.equal(root.get("fromAccountId"), value),
                    cb.equal(root.get("toAccountId"), value)
                );
                case "account_number", "accountnumber" -> cb.or(
                    cb.equal(root.get("fromAccountNumber"), value),
                    cb.equal(root.get("toAccountNumber"), value)
                );
                
                
                case "amount" -> createAmountPredicate(root, cb, "amount", value);
                case "min_amount", "minamount" -> cb.greaterThanOrEqualTo(root.get("amount"), new BigDecimal(value));
                case "max_amount", "maxamount" -> cb.lessThanOrEqualTo(root.get("amount"), new BigDecimal(value));
                case "currency" -> cb.equal(root.get("currency"), value);
                
                
                case "description" -> cb.like(cb.lower(root.get("description")), "%" + value.toLowerCase() + "%");
                case "notes" -> cb.like(cb.lower(root.get("notes")), "%" + value.toLowerCase() + "%");
                case "reference_number", "referencenumber" -> cb.equal(root.get("referenceNumber"), value);
                case "external_reference", "externalreference" -> cb.equal(root.get("externalReference"), value);
                case "external_provider", "externalprovider" -> cb.equal(root.get("externalProvider"), value);
                
                
                case "category_id", "categoryid" -> cb.equal(root.get("categoryId"), value);
                
                
                case "created_by", "createdby" -> cb.equal(root.get("createdBy"), value);
                case "updated_by", "updatedby" -> cb.equal(root.get("updatedBy"), value);
                
                
                case "created_after", "createdafter" -> cb.greaterThanOrEqualTo(root.get("createdAt"), parseDateTime(value));
                case "created_before", "createdbefore" -> cb.lessThanOrEqualTo(root.get("createdAt"), parseDateTime(value));
                case "updated_after", "updatedafter" -> cb.greaterThanOrEqualTo(root.get("updatedAt"), parseDateTime(value));
                case "updated_before", "updatedbefore" -> cb.lessThanOrEqualTo(root.get("updatedAt"), parseDateTime(value));
                
                
                case "date_range", "daterange" -> createDateRangePredicate(root, cb, "createdAt", value);
                case "created_date_range" -> createDateRangePredicate(root, cb, "createdAt", value);
                case "updated_date_range" -> createDateRangePredicate(root, cb, "updatedAt", value);
                
                
                case "types" -> createMultiValuePredicate(root, cb, "type", value, TransactionType.class);
                case "statuses" -> createMultiValuePredicate(root, cb, "status", value, TransactionStatus.class);
                case "directions" -> createMultiValuePredicate(root, cb, "direction", value, TransactionDirection.class);
                case "account_ids" -> createMultiValueInPredicate(root, cb, value, "fromAccountId", "toAccountId");
                case "account_numbers" -> createMultiValueInPredicate(root, cb, value, "fromAccountNumber", "toAccountNumber");
                
                
                case "has_external_reference" -> Boolean.parseBoolean(value) ? 
                    cb.isNotNull(root.get("externalReference")) : 
                    cb.isNull(root.get("externalReference"));
                case "has_notes" -> Boolean.parseBoolean(value) ? 
                    cb.isNotNull(root.get("notes")) : 
                    cb.isNull(root.get("notes"));
                case "has_category" -> Boolean.parseBoolean(value) ? 
                    cb.isNotNull(root.get("categoryId")) : 
                    cb.isNull(root.get("categoryId"));
                
                default -> {
                    log.warn("Unknown filter key: {}", key);
                    yield null;
                }
            };
        } catch (Exception e) {
            log.warn("Error creating predicate for key '{}' with value '{}': {}", key, value, e.getMessage());
            return null;
        }
    }

    private Specification<Transaction> buildSearchSpecification(String search) {
        if (search == null || search.trim().isEmpty()) {
            return null;
        }

        return (root, query, criteriaBuilder) -> {
            String searchTerm = "%" + search.toLowerCase() + "%";
            
            return criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), searchTerm),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("notes")), searchTerm),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("referenceNumber")), searchTerm),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("externalReference")), searchTerm),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("externalProvider")), searchTerm),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("fromAccountNumber")), searchTerm),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("toAccountNumber")), searchTerm)
            );
        };
    }

    
    private <T extends Enum<T>> Predicate createEnumPredicate(Root<Transaction> root, CriteriaBuilder cb,
                                                              String field, String value, Class<T> enumClass) {
        try {
            T enumValue = Enum.valueOf(enumClass, value.toUpperCase());
            return cb.equal(root.get(field), enumValue);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid enum value '{}' for field '{}' of type {}", value, field, enumClass.getSimpleName());
            return null;
        }
    }

    private <T extends Enum<T>> Predicate createMultiValuePredicate(Root<Transaction> root, CriteriaBuilder cb,
                                                                   String field, String value, Class<T> enumClass) {
        String[] values = value.split(",");
        List<T> enumValues = new ArrayList<>();
        
        for (String val : values) {
            try {
                enumValues.add(Enum.valueOf(enumClass, val.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid enum value '{}' for field '{}' of type {}", val, field, enumClass.getSimpleName());
            }
        }
        
        return enumValues.isEmpty() ? null : root.get(field).in(enumValues);
    }

    private Predicate createMultiValueInPredicate(Root<Transaction> root, CriteriaBuilder cb,
                                                 String value, String... fields) {
        String[] values = value.split(",");
        List<String> trimmedValues = Arrays.stream(values)
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .toList();
        
        if (trimmedValues.isEmpty()) {
            return null;
        }
        
        List<Predicate> predicates = new ArrayList<>();
        for (String field : fields) {
            predicates.add(root.get(field).in(trimmedValues));
        }
        
        return cb.or(predicates.toArray(new Predicate[0]));
    }

    private Predicate createAmountPredicate(Root<Transaction> root, CriteriaBuilder cb,
                                           String field, String value) {
        try {
            if (value.contains(",")) {
                String[] range = value.split(",");
                BigDecimal min = new BigDecimal(range[0].trim());
                BigDecimal max = new BigDecimal(range[1].trim());
                return cb.between(root.get(field), min, max);
            } else if (value.startsWith(">=")) {
                BigDecimal amount = new BigDecimal(value.substring(2).trim());
                return cb.greaterThanOrEqualTo(root.get(field), amount);
            } else if (value.startsWith("<=")) {
                BigDecimal amount = new BigDecimal(value.substring(2).trim());
                return cb.lessThanOrEqualTo(root.get(field), amount);
            } else if (value.startsWith(">")) {
                BigDecimal amount = new BigDecimal(value.substring(1).trim());
                return cb.greaterThan(root.get(field), amount);
            } else if (value.startsWith("<")) {
                BigDecimal amount = new BigDecimal(value.substring(1).trim());
                return cb.lessThan(root.get(field), amount);
            } else {
                BigDecimal amount = new BigDecimal(value.trim());
                return cb.equal(root.get(field), amount);
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid amount format: {}", value);
            return null;
        }
    }

    private Predicate createDateRangePredicate(Root<Transaction> root, CriteriaBuilder cb,
                                              String field, String value) {
        try {
            String[] dates = value.split(",");
            if (dates.length != 2) {
                log.warn("Date range must have two dates separated by comma: {}", value);
                return null;
            }
            
            LocalDateTime startDate = parseDateTime(dates[0].trim());
            LocalDateTime endDate = parseDateTime(dates[1].trim());
            
            return cb.between(root.get(field), startDate, endDate);
        } catch (Exception e) {
            log.warn("Invalid date range format: {}", value);
            return null;
        }
    }

    private LocalDateTime parseDateTime(String dateStr) {
        try {
            if (dateStr.length() <= 10) {
                return LocalDate.parse(dateStr).atStartOfDay();
            } else {
                dateStr = dateStr.replace(" ", "T");
                return LocalDateTime.parse(dateStr);
            }
        } catch (Exception e) {
            log.warn("Failed to parse date: {}", dateStr);
            throw new IllegalArgumentException("Invalid date format: " + dateStr);
        }
    }
}