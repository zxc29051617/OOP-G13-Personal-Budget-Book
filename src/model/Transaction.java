package model;

import java.time.LocalDate;

public class Transaction {
    public enum Kind {
        INCOME, EXPENSE
    }

    private final int id;
    private LocalDate date;
    private Kind kind;
    private String category;
    private int accountId;
    private double amount;
    private String note;

    public Transaction(int id, LocalDate date, Kind kind, String category, int accountId, double amount, String note) {
        this.id = id;
        this.date = date;
        this.kind = kind;
        this.category = category;
        this.accountId = accountId;
        this.amount = amount;
        this.note = note;
    }

    public int getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
