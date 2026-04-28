package service;

import model.Account;
import model.Transaction;
import persistence.BudgetBookStore;

import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StatsService {
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

    public Map<String, Double> monthlyExpenseByCategory(YearMonth month) {
        Map<String, Double> result = new LinkedHashMap<>();
        List<Transaction> rows = store.getTransactions();
        for (Transaction transaction : rows) {
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
