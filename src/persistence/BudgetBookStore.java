package persistence;

import model.Account;
import model.BudgetSettings;
import model.Transaction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BudgetBookStore {
    private final Path dataDir;
    private final Path accountsFile;
    private final Path transactionsFile;
    private final Path settingsFile;

    private final List<Account> accounts = new ArrayList<>();
    private final List<Transaction> transactions = new ArrayList<>();
    private BudgetSettings settings = new BudgetSettings(10000, 0);
    private int nextAccountId = 1;
    private int nextTransactionId = 1;

    public BudgetBookStore(Path dataDir) {
        this.dataDir = dataDir;
        this.accountsFile = dataDir.resolve("accounts.csv");
        this.transactionsFile = dataDir.resolve("transactions.csv");
        this.settingsFile = dataDir.resolve("settings.csv");
    }

    public void load() throws IOException {
        Files.createDirectories(dataDir);
        loadAccounts();
        loadTransactions();
        loadSettings();
        if (accounts.isEmpty()) {
            addAccount("現金", "現金", 3000);
            addAccount("銀行帳戶", "銀行", 15000);
            addAccount("電子支付", "數位支付", 1000);
            save();
        }
    }

    public void save() throws IOException {
        saveAccounts();
        saveTransactions();
        saveSettings();
    }

    public List<Account> getAccounts() {
        return new ArrayList<>(accounts);
    }

    public List<Transaction> getTransactions() {
        List<Transaction> copy = new ArrayList<>(transactions);
        copy.sort(Comparator.comparing(Transaction::getDate).reversed().thenComparing(Transaction::getId).reversed());
        return copy;
    }

    public BudgetSettings getSettings() {
        return settings;
    }

    public Account addAccount(String name, String type, double initialBalance) {
        Account account = new Account(nextAccountId++, name, type, initialBalance);
        accounts.add(account);
        return account;
    }

    public void deleteAccount(int accountId) {
        boolean used = transactions.stream().anyMatch(t -> t.getAccountId() == accountId);
        if (used) {
            throw new IllegalArgumentException("此帳戶已有交易紀錄，不能刪除。");
        }
        accounts.removeIf(a -> a.getId() == accountId);
    }

    public Transaction addTransaction(LocalDate date, Transaction.Kind kind, String category, int accountId, double amount, String note) {
        Transaction transaction = new Transaction(nextTransactionId++, date, kind, category, accountId, amount, note);
        transactions.add(transaction);
        settings.addPoints(10);
        return transaction;
    }

    public void deleteTransaction(int id) {
        transactions.removeIf(t -> t.getId() == id);
    }

    public Account findAccount(int id) {
        for (Account account : accounts) {
            if (account.getId() == id) {
                return account;
            }
        }
        return null;
    }

    public double getAccountBalance(int accountId) {
        Account account = findAccount(accountId);
        double balance = account == null ? 0 : account.getInitialBalance();
        for (Transaction transaction : transactions) {
            if (transaction.getAccountId() == accountId) {
                balance += transaction.getKind() == Transaction.Kind.INCOME
                        ? transaction.getAmount()
                        : -transaction.getAmount();
            }
        }
        return balance;
    }

    private void loadAccounts() throws IOException {
        accounts.clear();
        if (!Files.exists(accountsFile)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(accountsFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> row = CsvUtil.parse(line);
                if (row.size() >= 4) {
                    int id = Integer.parseInt(row.get(0));
                    accounts.add(new Account(id, row.get(1), row.get(2), Double.parseDouble(row.get(3))));
                    nextAccountId = Math.max(nextAccountId, id + 1);
                }
            }
        }
    }

    private void loadTransactions() throws IOException {
        transactions.clear();
        if (!Files.exists(transactionsFile)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(transactionsFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                List<String> row = CsvUtil.parse(line);
                if (row.size() >= 7) {
                    int id = Integer.parseInt(row.get(0));
                    transactions.add(new Transaction(
                            id,
                            LocalDate.parse(row.get(1)),
                            Transaction.Kind.valueOf(row.get(2)),
                            row.get(3),
                            Integer.parseInt(row.get(4)),
                            Double.parseDouble(row.get(5)),
                            row.get(6)));
                    nextTransactionId = Math.max(nextTransactionId, id + 1);
                }
            }
        }
    }

    private void loadSettings() throws IOException {
        if (!Files.exists(settingsFile)) {
            return;
        }
        List<String> lines = Files.readAllLines(settingsFile, StandardCharsets.UTF_8);
        if (!lines.isEmpty()) {
            List<String> row = CsvUtil.parse(lines.get(0));
            if (row.size() >= 2) {
                settings = new BudgetSettings(Double.parseDouble(row.get(0)), Integer.parseInt(row.get(1)));
            }
        }
    }

    private void saveAccounts() throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(accountsFile, StandardCharsets.UTF_8)) {
            for (Account account : accounts) {
                writer.write(CsvUtil.format(List.of(
                        String.valueOf(account.getId()),
                        account.getName(),
                        account.getType(),
                        String.valueOf(account.getInitialBalance()))));
                writer.newLine();
            }
        }
    }

    private void saveTransactions() throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(transactionsFile, StandardCharsets.UTF_8)) {
            for (Transaction transaction : transactions) {
                writer.write(CsvUtil.format(List.of(
                        String.valueOf(transaction.getId()),
                        transaction.getDate().toString(),
                        transaction.getKind().name(),
                        transaction.getCategory(),
                        String.valueOf(transaction.getAccountId()),
                        String.valueOf(transaction.getAmount()),
                        transaction.getNote())));
                writer.newLine();
            }
        }
    }

    private void saveSettings() throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(settingsFile, StandardCharsets.UTF_8)) {
            writer.write(CsvUtil.format(List.of(
                    String.valueOf(settings.getMonthlyLimit()),
                    String.valueOf(settings.getPoints()))));
            writer.newLine();
        }
    }
}
