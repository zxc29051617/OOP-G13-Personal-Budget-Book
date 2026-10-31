package budgetbook.model;

/** 錢包種類，決定預設圖示文字。 */
public enum WalletType {
    CASH("現金", "現"),
    BANK("銀行帳戶", "銀"),
    E_PAYMENT("電子支付", "電"),
    CREDIT_CARD("信用卡", "卡"),
    INVESTMENT("投資帳戶", "投"),
    OTHER("其他", "其");

    private final String displayName;
    private final String icon;

    WalletType(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String displayName() {
        return displayName;
    }

    public String icon() {
        return icon;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
