package model;

import java.time.LocalDate;
import java.time.LocalTime;

public class Transaction {
    public enum Kind {
        INCOME, EXPENSE
    }

    private final int id;
    private LocalDate date;
    private LocalTime time;
    private Kind kind;
    private String category;
    private int accountId;
    private double amount;
    private String note;

    public Transaction(int id, LocalDate date, Kind kind, String category, int accountId, double amount, String note) {
        this(id, date, LocalTime.MIDNIGHT, kind, category, accountId, amount, note);
    }

    public Transaction(int id, LocalDate date, LocalTime time, Kind kind, String category, int accountId, double amount, String note) {
        this.id = id;
        this.date = date;
        this.time = time == null ? LocalTime.MIDNIGHT : time;
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

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time == null ? LocalTime.MIDNIGHT : time;
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
