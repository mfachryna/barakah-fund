package com.barakah.transaction.service;

import com.barakah.transaction.entity.TransactionCategory;
import com.barakah.transaction.exception.TransactionExceptions;
import com.barakah.transaction.repository.TransactionCategoryRepository;
import com.barakah.shared.context.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TransactionCategoryService {

    private final TransactionCategoryRepository categoryRepository;

    @Transactional
    @CachePut(value = "transaction-categories", key = "#result.categoryId")
    @CacheEvict(value = {"active-categories", "system-categories"}, allEntries = true)
    public TransactionCategory createCategory(String name, String description, String icon, String color) {
        log.info("Creating transaction category: {}", name);

        if (name == null || name.trim().isEmpty()) {
            throw new TransactionExceptions.InvalidTransactionException("Category name is required");
        }

        if (categoryRepository.existsByName(name.trim())) {
            throw new TransactionExceptions.InvalidTransactionException("Category name already exists: " + name);
        }

        String currentUserId = UserContextHolder.getCurrentUserId();

        TransactionCategory category = TransactionCategory.builder()
                .name(name.trim())
                .description(description)
                .icon(icon)
                .color(color)
                .isActive(true)
                .isSystem(false)
                .createdBy(currentUserId)
                .build();

        TransactionCategory savedCategory = categoryRepository.save(category);
        log.info("Transaction category created: {} with ID: {}", name, savedCategory.getCategoryId());

        return savedCategory;
    }

    @Transactional
    @Caching(
        put = @CachePut(value = "transaction-categories", key = "#categoryId"),
        evict = {
            @CacheEvict(value = "active-categories", allEntries = true),
            @CacheEvict(value = "system-categories", allEntries = true)
        }
    )
    public TransactionCategory updateCategory(String categoryId, String name, String description,
            String icon, String color, Boolean isActive) {
        log.info("Updating transaction category: {}", categoryId);

        TransactionCategory category = getCategoryById(categoryId);

        if (category.getIsSystem()) {
            throw new TransactionExceptions.SystemCategoryException("modify");
        }

        if (name != null && !name.trim().isEmpty()) {
            if (!category.getName().equals(name.trim()) && categoryRepository.existsByName(name.trim())) {
                throw new TransactionExceptions.InvalidTransactionException("Category name already exists: " + name);
            }
            category.setName(name.trim());
        }

        if (description != null) {
            category.setDescription(description);
        }

        if (icon != null) {
            category.setIcon(icon);
        }

        if (color != null) {
            category.setColor(color);
        }

        if (isActive != null) {
            category.setIsActive(isActive);
        }

        TransactionCategory updatedCategory = categoryRepository.save(category);
        log.info("Transaction category updated: {}", categoryId);

        return updatedCategory;
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "transaction-categories", key = "#categoryId"),
        @CacheEvict(value = "active-categories", allEntries = true),
        @CacheEvict(value = "system-categories", allEntries = true)
    })
    public void deleteCategory(String categoryId) {
        log.info("Deleting transaction category: {}", categoryId);

        TransactionCategory category = getCategoryById(categoryId);

        if (category.getIsSystem()) {
            throw new TransactionExceptions.SystemCategoryException("delete");
        }

        long transactionCount = categoryRepository.countTransactionsByCategoryId(categoryId);
        if (transactionCount > 0) {
            throw new TransactionExceptions.CategoryInUseException(categoryId);
        }

        categoryRepository.delete(category);
        log.info("Transaction category deleted: {}", categoryId);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "transaction-categories", key = "#categoryId")
    public TransactionCategory getCategoryById(String categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new TransactionExceptions.CategoryNotFoundException(categoryId));
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "active-categories")
    public List<TransactionCategory> getActiveCategories() {
        log.debug("Fetching active categories from database");
        return categoryRepository.findByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "system-categories")
    public List<TransactionCategory> getSystemCategories() {
        log.debug("Fetching system categories from database");
        return categoryRepository.findByIsSystemTrue();
    }

    @Transactional(readOnly = true)
    public Page<TransactionCategory> listCategories(boolean includeInactive, boolean includeSystem, Pageable pageable) {
        return categoryRepository.findCategoriesWithFilters(includeInactive, includeSystem, pageable);
    }

    /**
     * Initialize default system categories
     */
    @Transactional
    @CacheEvict(value = {"active-categories", "system-categories"}, allEntries = true)
    public void initializeSystemCategories() {
        log.info("Initializing system transaction categories");

        createSystemCategoryIfNotExists("Transfer", "Money transfer between accounts", "transfer", "#2196F3");
        createSystemCategoryIfNotExists("Deposit", "Money deposit", "deposit", "#4CAF50");
        createSystemCategoryIfNotExists("Withdrawal", "Money withdrawal", "withdrawal", "#FF9800");
        createSystemCategoryIfNotExists("Payment", "Payment to external parties", "payment", "#F44336");
        createSystemCategoryIfNotExists("Refund", "Refund from external parties", "refund", "#9C27B0");
        createSystemCategoryIfNotExists("Fee", "Service fees", "fee", "#607D8B");
        createSystemCategoryIfNotExists("Interest", "Interest earned or paid", "interest", "#00BCD4");
        createSystemCategoryIfNotExists("Food & Dining", "Restaurant, grocery, etc.", "restaurant", "#FF5722");
        createSystemCategoryIfNotExists("Transportation", "Gas, public transport, etc.", "car", "#795548");
        createSystemCategoryIfNotExists("Shopping", "Clothing, electronics, etc.", "shopping", "#E91E63");
        createSystemCategoryIfNotExists("Entertainment", "Movies, games, etc.", "entertainment", "#9C27B0");
        createSystemCategoryIfNotExists("Bills & Utilities", "Electricity, water, internet, etc.", "bill", "#FF9800");
        createSystemCategoryIfNotExists("Healthcare", "Medical expenses", "health", "#4CAF50");
        createSystemCategoryIfNotExists("Education", "School, courses, books", "education", "#2196F3");
        createSystemCategoryIfNotExists("Investment", "Stocks, bonds, etc.", "investment", "#FF9800");
        createSystemCategoryIfNotExists("Salary", "Monthly salary", "salary", "#4CAF50");
        createSystemCategoryIfNotExists("Other", "Miscellaneous transactions", "other", "#9E9E9E");

        log.info("System transaction categories initialized");
    }

    private void createSystemCategoryIfNotExists(String name, String description, String icon, String color) {
        if (!categoryRepository.existsByName(name)) {
            TransactionCategory category = TransactionCategory.builder()
                    .name(name)
                    .description(description)
                    .icon(icon)
                    .color(color)
                    .isActive(true)
                    .isSystem(true)
                    .createdBy("SYSTEM")
                    .build();

            categoryRepository.save(category);
            log.debug("Created system category: {}", name);
        }
    }
}
