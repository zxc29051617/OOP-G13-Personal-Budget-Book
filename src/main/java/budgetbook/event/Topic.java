package budgetbook.event;

/** 資料變更的種類，畫面依此決定要不要重新整理。 */
public enum Topic {
    TRANSACTIONS,
    WALLETS,
    CATEGORIES,
    BUDGETS,
    RECURRING,
    GOALS,
    SETTINGS
}
