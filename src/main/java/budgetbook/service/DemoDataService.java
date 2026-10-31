package budgetbook.service;

import budgetbook.command.CommandManager;
import budgetbook.event.DataChangedEvent;
import budgetbook.event.EventBus;
import budgetbook.model.Category;
import budgetbook.model.CategoryKind;
import budgetbook.model.Money;
import budgetbook.model.Wallet;
import budgetbook.model.WalletType;
import budgetbook.repository.Storage;

import java.time.Clock;
import java.time.LocalDate;

/** 第一次啟動時建立預設分類與錢包，或清除資料後載入一份示範資料。 */
public final class DemoDataService {
    /** 預設支出分類：名稱、顏色、圖示。沿用原版的分類並補上居住與學習。 */
    static final Object[][] EXPENSE_CATEGORIES = {
            {"餐飲", 0xE76F51, "餐"}, {"交通", 0x457B9D, "交"}, {"購物", 0xD16BA5, "購"},
            {"娛樂", 0x9B5DE5, "娛"}, {"居住", 0x2A9D8F, "住"}, {"生活", 0x8AB17D, "生"},
            {"醫療", 0xE9C46A, "醫"}, {"學習", 0x5E60CE, "學"}, {"其他", 0x6C757D, "其"}};
    static final Object[][] INCOME_CATEGORIES = {
            {"薪資", 0x4C9A62, "薪"}, {"打工", 0xF4A261, "工"}, {"獎金", 0xE9C46A, "獎"},
            {"投資", 0x457B9D, "投"}, {"其他", 0x6C757D, "其"}};

    private final Storage storage;
    private final CommandManager commands;
    private final EventBus bus;
    private final Clock clock;

    public DemoDataService(Storage storage, CommandManager commands, EventBus bus, Clock clock) {
        this.storage = storage;
        this.commands = commands;
        this.bus = bus;
        this.clock = clock;
    }

    public boolean isEmpty() {
        return storage.transactions().count() == 0;
    }

    /**
     * 資料庫完全沒有分類與錢包時，建立預設資料（原版的「現金、銀行帳戶、電子支付」三個錢包）。
     *
     * @return 是否有建立
     */
    public boolean ensureStarterData() {
        if (storage.categories().count() > 0 || storage.wallets().count() > 0) {
            return false;
        }
        storage.inTransaction(() -> {
            createDefaultCategories(storage);
            storage.wallets().save(Wallet.create("現金", WalletType.CASH, Money.of(3000), 0x4C9A62));
            storage.wallets().save(Wallet.create("銀行帳戶", WalletType.BANK, Money.of(15000), 0x457B9D));
            storage.wallets().save(Wallet.create("電子支付", WalletType.E_PAYMENT, Money.of(1000), 0xF4A261));
        });
        bus.publish(DataChangedEvent.everything());
        return true;
    }

    /** 清除所有記帳資料（保留設定）並載入示範資料。這個動作無法復原，所以會清空復原歷史。 */
    public void loadDemoData() {
        LocalDate today = LocalDate.now(clock);
        storage.inTransaction(() -> {
            storage.clearAllData();
            new DemoDataGenerator(storage, today).generate();
        });
        commands.clearHistory();
        bus.publish(DataChangedEvent.everything());
    }

    static void createDefaultCategories(Storage storage) {
        for (Object[] row : EXPENSE_CATEGORIES) {
            storage.categories().save(Category.create((String) row[0], CategoryKind.EXPENSE, (Integer) row[1],
                    (String) row[2]));
        }
        for (Object[] row : INCOME_CATEGORIES) {
            storage.categories().save(Category.create((String) row[0], CategoryKind.INCOME, (Integer) row[1],
                    (String) row[2]));
        }
    }
}
