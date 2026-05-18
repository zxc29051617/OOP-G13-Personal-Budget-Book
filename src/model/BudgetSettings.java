package model;

public class BudgetSettings {
    private double monthlyLimit;
    private int points;

    public BudgetSettings(double monthlyLimit, int points) {
        this.monthlyLimit = monthlyLimit;
        this.points = points;
    }

    public double getMonthlyLimit() {
        return monthlyLimit;
    }

    public void setMonthlyLimit(double monthlyLimit) {
        this.monthlyLimit = monthlyLimit;
    }

    public int getPoints() {
        return points;
    }

    public void addPoints(int amount) {
        points += amount;
    }

    public void setPoints(int points) {
        this.points = points;
    }
}
