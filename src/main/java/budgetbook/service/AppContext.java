package budgetbook.service;

import budgetbook.command.CommandManager;
import budgetbook.event.EventBus;
import budgetbook.repository.Storage;
import budgetbook.repository.StorageConfig;
import budgetbook.repository.StorageFactory;
import budgetbook.service.assistant.AssistantServices;
import budgetbook.service.assistant.QueryAssistantService;
import budgetbook.service.io.TransactionCsvService;
import budgetbook.service.report.ReportService;

import java.time.Clock;

/**
 * 組合根（Composition Root）：建立資料庫、事件匯流排、指令管理員與所有 service，
 * 並以建構子注入彼此的相依物件。UI 與測試都從這裡取得 service。
 */
public final class AppContext implements AutoCloseable {
    private final StorageConfig config;
    private final Storage storage;
    private final Clock clock;
    private final EventBus bus = new EventBus();
    private final CommandManager commands;
    private final SettingsService settings;
    private final WalletService wallets;
    private final CategoryService categories;
    private final TransactionService transactions;
    private final BudgetService budgets;
    private final RecurringService recurring;
    private final SavingsGoalService goals;
    private final StatisticsService statistics;
    private final TreeService trees;
    private final ReportService reports;
    private final TransactionCsvService csv;
    private final QueryAssistantService assistant;
    private final DemoDataService demo;

    private AppContext(StorageConfig config, Storage storage, Clock clock) {
        this.config = config;
        this.storage = storage;
        this.clock = clock;
        this.commands = new CommandManager(storage, bus);
        this.settings = new SettingsService(storage.settings(), bus);
        this.wallets = new WalletService(storage, commands);
        this.categories = new CategoryService(storage, commands);
        this.transactions = new TransactionService(storage, commands, clock);
        this.budgets = new BudgetService(storage, commands, clock);
        this.recurring = new RecurringService(storage, commands, clock);
        this.goals = new SavingsGoalService(storage, commands);
        this.statistics = new StatisticsService(storage, wallets, clock);
        this.trees = new TreeService(storage, settings, clock);
        this.reports = new ReportService(storage, statistics, budgets, clock);
        this.csv = new TransactionCsvService(storage, commands);
        this.assistant = new QueryAssistantService(new AssistantServices(statistics, budgets, wallets, categories,
                transactions, goals, recurring, trees, clock));
        this.demo = new DemoDataService(storage, commands, bus, clock);
    }

    public static AppContext open(StorageConfig config) {
        return open(config, Clock.systemDefaultZone());
    }

    public static AppContext open(StorageConfig config, Clock clock) {
        return new AppContext(config, StorageFactory.open(config), clock);
    }

    public StorageConfig config() {
        return config;
    }

    public Storage storage() {
        return storage;
    }

    public Clock clock() {
        return clock;
    }

    public EventBus bus() {
        return bus;
    }

    public CommandManager commands() {
        return commands;
    }

    public SettingsService settings() {
        return settings;
    }

    public WalletService wallets() {
        return wallets;
    }

    public CategoryService categories() {
        return categories;
    }

    public TransactionService transactions() {
        return transactions;
    }

    public BudgetService budgets() {
        return budgets;
    }

    public RecurringService recurring() {
        return recurring;
    }

    public SavingsGoalService goals() {
        return goals;
    }

    public StatisticsService statistics() {
        return statistics;
    }

    public TreeService trees() {
        return trees;
    }

    public ReportService reports() {
        return reports;
    }

    public TransactionCsvService csv() {
        return csv;
    }

    public QueryAssistantService assistant() {
        return assistant;
    }

    public DemoDataService demo() {
        return demo;
    }

    @Override
    public void close() {
        storage.close();
    }
}
