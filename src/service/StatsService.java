package service;

import model.Account;
import model.Transaction;
import persistence.BudgetBookStore;

import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;

public class StatsService {
    public static final int MONTHLY_TREE_GOAL = 20;

    private final BudgetBookStore store;

    public StatsService(BudgetBookStore store) {
        this.store = store;
    }

    public double totalBalance() {
        double total = 0;
        for (Account account : store.getAccounts()) {
            total += store.getAccountBalance(account.getId());
        }
        return total;
    }

    public double monthlyIncome(YearMonth month) {
        return sumByKind(month, Transaction.Kind.INCOME);
    }

    public double monthlyExpense(YearMonth month) {
        return sumByKind(month, Transaction.Kind.EXPENSE);
    }

    public int monthlyWaterCount(YearMonth month) {
        int count = 0;
        for (Transaction transaction : store.getTransactions()) {
            if (YearMonth.from(transaction.getDate()).equals(month)) {
                count++;
            }
        }
        return count;
    }

    public int completedTreeCount() {
        Map<YearMonth, Integer> countByMonth = new HashMap<>();
        for (Transaction transaction : store.getTransactions()) {
            YearMonth month = YearMonth.from(transaction.getDate());
            countByMonth.put(month, countByMonth.getOrDefault(month, 0) + 1);
        }

        int completed = 0;
        for (int count : countByMonth.values()) {
            if (count >= MONTHLY_TREE_GOAL) {
                completed++;
            }
        }
        return completed;
    }

    public String monthlyTreeStatus(YearMonth month) {
        int waterCount = monthlyWaterCount(month);
        if (waterCount >= MONTHLY_TREE_GOAL) {
            return "完成一棵樹";
        }
        if (waterCount == 0) {
            return "枯萎";
        }
        return "成長中";
    }

    public int monthlyTreeProgressPercent(YearMonth month) {
        int waterCount = monthlyWaterCount(month);
        return Math.min(100, Math.round(waterCount * 100f / MONTHLY_TREE_GOAL));
    }

    public Map<String, Double> monthlyExpenseByCategory(YearMonth month) {
        Map<String, Double> result = new LinkedHashMap<>();
        for (Transaction transaction : store.getTransactions()) {
            if (transaction.getKind() == Transaction.Kind.EXPENSE && YearMonth.from(transaction.getDate()).equals(month)) {
                result.put(transaction.getCategory(), result.getOrDefault(transaction.getCategory(), 0.0) + transaction.getAmount());
            }
        }
        return result;
    }

    private double sumByKind(YearMonth month, Transaction.Kind kind) {
        double total = 0;
        for (Transaction transaction : store.getTransactions()) {
            if (transaction.getKind() == kind && YearMonth.from(transaction.getDate()).equals(month)) {
                total += transaction.getAmount();
            }
        }
        return total;
    }
}
