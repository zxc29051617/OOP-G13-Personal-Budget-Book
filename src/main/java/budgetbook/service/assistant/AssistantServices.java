package budgetbook.service.assistant;

import budgetbook.service.BudgetService;
import budgetbook.service.CategoryService;
import budgetbook.service.RecurringService;
import budgetbook.service.SavingsGoalService;
import budgetbook.service.StatisticsService;
import budgetbook.service.TransactionService;
import budgetbook.service.TreeService;
import budgetbook.service.WalletService;

import java.time.Clock;
import java.time.LocalDate;

/** 助手回答問題時會用到的 service。 */
public record AssistantServices(StatisticsService statistics, BudgetService budgets, WalletService wallets,
                                CategoryService categories, TransactionService transactions,
                                SavingsGoalService goals, RecurringService recurring, TreeService trees,
                                Clock clock) {
    public LocalDate today() {
        return LocalDate.now(clock);
    }
}
