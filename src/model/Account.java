package model;

public class Account {
    private final int id;
    private String name;
    private String type;
    private double initialBalance;

    public Account(int id, String name, String type, double initialBalance) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.initialBalance = initialBalance;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(double initialBalance) {
        this.initialBalance = initialBalance;
    }

    @Override
    public String toString() {
        return name + " (" + type + ")";
    }
}
