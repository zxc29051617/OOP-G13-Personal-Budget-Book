package budgetbook.model;

/** 單一分類在某段期間的金額、筆數與占比。 */
public record CategoryTotal(Category category, Money total, int count, double share) {
    public int sharePercent() {
        return (int) Math.round(share * 100);
    }
}
