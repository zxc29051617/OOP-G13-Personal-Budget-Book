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

    public String getTreeLevel() {
        if (points >= 500) {
            return "Lv.5 記帳達人";
        }
        if (points >= 300) {
            return "Lv.4 預算高手";
        }
        if (points >= 150) {
            return "Lv.3 穩定累積";
        }
        if (points >= 50) {
            return "Lv.2 開始養成";
        }
        return "Lv.1 新手上路";
    }
}
