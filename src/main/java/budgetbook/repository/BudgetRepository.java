package budgetbook.repository;

import budgetbook.model.Budget;

import java.util.Optional;

public interface BudgetRepository extends Repository<Budget> {
    /** {@code categoryId} 為 null 時找每月總預算。 */
    Optional<Budget> findByCategoryId(Long categoryId);
}
