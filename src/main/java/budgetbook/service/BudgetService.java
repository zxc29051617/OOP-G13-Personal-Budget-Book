package budgetbook.service;

import budgetbook.command.CommandManager;
import budgetbook.command.DeleteCommand;
import budgetbook.command.SaveCommand;
import budgetbook.event.Topic;
import budgetbook.exception.ValidationException;
import budgetbook.model.Budget;
import budgetbook.model.BudgetLevel;
import budgetbook.model.BudgetStatus;
import budgetbook.model.Category;
import budgetbook.model.CategoryKind;
import budgetbook.model.DateRange;
import budgetbook.model.Money;
import budgetbook.model.Transaction;
import budgetbook.repository.Storage;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 每月總預算與各分類預算：設定、使用率計算與超支提醒。 */
public final class BudgetService {
    private final Storage storage;
    private final CommandManager commands;
    private final Clock clock;

    public BudgetService(Storage storage, CommandManager commands, Clock clock) {
        this.storage = storage;
        this.commands = commands;
        this.clock = clock;
    }

    public List<Budget> all() {
        return storage.budgets().findAll();
    }

    public Optional<Budget> overall() {
        return storage.budgets().findByCategoryId(null);
    }

    public Optional<Budget> forCategory(long categoryId) {
        return storage.budgets().findByCategoryId(categoryId);
    }

    /** 設定每月總預算；傳入 null 或 0 代表取消。 */
    public void setOverall(Money limit) {
        set(null, limit, "每月總預算");
    }

    /** 設定支出分類的每月預算；傳入 null 或 0 代表取消。 */
    public void setForCategory(long categoryId, Money limit) {
        Category category = storage.categories().getById(categoryId);
        if (category.kind() != CategoryKind.EXPENSE) {
            throw new ValidationException("分類", "只有支出分類可以設定預算。");
        }
        set(categoryId, limit, "「" + category.name() + "」預算");
    }

    private void set(Long categoryId, Money limit, String label) {
        Optional<Budget> existing = storage.budgets().findByCategoryId(categoryId);
        if (limit == null || limit.isZero()) {
            existing.ifPresent(budget -> commands.execute(new DeleteCommand<>(storage.budgets(), budget.id(),
                    "取消" + label, Topic.BUDGETS)));
            return;
        }
        if (limit.isNegative()) {
            throw new ValidationException("預算金額", "預算金額不能是負數。");
        }
        Budget budget = existing.map(b -> b.withLimit(limit)).orElseGet(() -> new Budget(0, categoryId, limit));
        commands.execute(new SaveCommand<>(storage.budgets(), budget, "設定" + label + " " + limit.format(),
                Topic.BUDGETS));
    }

    /** 某月所有預算的使用情況：總預算在最前面，其餘依使用率由高到低。 */
    public List<BudgetStatus> statuses(YearMonth month) {
        List<Budget> budgets = all();
        if (budgets.isEmpty()) {
            return List.of();
        }
        List<Transaction> expenses = storage.transactions().findBetween(DateRange.ofMonth(month)).stream()
                .filter(Transaction::isExpense).toList();
        Money total = Money.sum(expenses.stream().map(Transaction::amount));
        Map<Long, Money> byCategory = new HashMap<>();
        expenses.forEach(t -> byCategory.merge(t.categoryId(), t.amount(), Money::plus));
        Map<Long, Category> categories = new HashMap<>();
        storage.categories().findAll().forEach(c -> categories.put(c.id(), c));

        List<BudgetStatus> result = new ArrayList<>();
        List<BudgetStatus> categoryStatuses = new ArrayList<>();
        for (Budget budget : budgets) {
            if (budget.isOverall()) {
                result.add(new BudgetStatus(budget, null, month, total));
            } else {
                categoryStatuses.add(new BudgetStatus(budget, categories.get(budget.categoryId()), month,
                        byCategory.getOrDefault(budget.categoryId(), Money.ZERO)));
            }
        }
        categoryStatuses.sort(Comparator.comparingDouble(BudgetStatus::ratio).reversed());
        result.addAll(categoryStatuses);
        return result;
    }

    public Optional<BudgetStatus> overallStatus(YearMonth month) {
        return statuses(month).stream().filter(BudgetStatus::isOverall).findFirst();
    }

    /** 使用率 80% 以上或已超支的預算。 */
    public List<BudgetStatus> alerts(YearMonth month) {
        return statuses(month).stream().filter(status -> status.level() != BudgetLevel.OK).toList();
    }

    /** 依目前花費速度推估月底支出；過去月份回傳實際支出，未來月份回傳 0。 */
    public Money projectedSpending(YearMonth month, Money spentSoFar) {
        LocalDate today = LocalDate.now(clock);
        YearMonth current = YearMonth.from(today);
        if (month.isBefore(current)) {
            return spentSoFar;
        }
        if (month.isAfter(current)) {
            return Money.ZERO;
        }
        return spentSoFar.times(month.lengthOfMonth()).dividedBy(today.getDayOfMonth()).roundToDollar();
    }
}
