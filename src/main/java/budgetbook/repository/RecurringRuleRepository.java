package budgetbook.repository;

import budgetbook.model.RecurringRule;

public interface RecurringRuleRepository extends Repository<RecurringRule> {
    long countByWallet(long walletId);

    long countByCategory(long categoryId);
}
