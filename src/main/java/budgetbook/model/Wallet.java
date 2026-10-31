package budgetbook.model;

/**
 * 錢包（現金、銀行帳戶、電子支付…）。目前餘額 = 初始金額 + 所有相關交易的影響，
 * 由 service 計算，不直接存在資料庫，避免資料不一致。
 *
 * @param archived 封存後不出現在記帳選單，但保留歷史紀錄
 */
public record Wallet(long id, String name, WalletType type, Money initialBalance, int color, boolean archived)
        implements Entity {
    public static final int MAX_NAME_LENGTH = 30;

    public Wallet {
        name = Checks.requireText(name, "錢包名稱", MAX_NAME_LENGTH);
        Checks.requireValue(type, "錢包類型");
        Checks.requireValue(initialBalance, "初始金額");
        color = color & 0xFFFFFF;
    }

    public static Wallet create(String name, WalletType type, Money initialBalance, int color) {
        return new Wallet(0, name, type, initialBalance, color, false);
    }

    public Wallet withId(long newId) {
        return new Wallet(newId, name, type, initialBalance, color, archived);
    }

    public Wallet withName(String newName) {
        return new Wallet(id, newName, type, initialBalance, color, archived);
    }

    public Wallet withArchived(boolean value) {
        return new Wallet(id, name, type, initialBalance, color, value);
    }

    public String icon() {
        return type.icon();
    }

    @Override
    public String toString() {
        return name;
    }
}
