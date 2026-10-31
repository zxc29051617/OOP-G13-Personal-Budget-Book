package budgetbook.service;

import budgetbook.command.CommandManager;
import budgetbook.command.DeleteCommand;
import budgetbook.command.SaveCommand;
import budgetbook.event.Topic;
import budgetbook.exception.ValidationException;
import budgetbook.model.Category;
import budgetbook.model.CategoryKind;
import budgetbook.model.Transaction;
import budgetbook.model.TransactionQuery;
import budgetbook.model.TransactionType;
import budgetbook.model.Wallet;
import budgetbook.repository.Storage;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 交易的新增、修改、刪除、查詢，以及依備註推薦分類。 */
public final class TransactionService {
    private final Storage storage;
    private final CommandManager commands;
    private final Clock clock;

    public TransactionService(Storage storage, CommandManager commands, Clock clock) {
        this.storage = storage;
        this.commands = commands;
        this.clock = clock;
    }

    public Transaction add(Transaction transaction) {
        if (!transaction.isNew()) {
            throw new IllegalArgumentException("Use update() for existing transactions");
        }
        validateReferences(transaction, true);
        return commands.execute(new SaveCommand<>(storage.transactions(), transaction,
                describe("新增", transaction), Topic.TRANSACTIONS)).result();
    }

    public Transaction update(Transaction transaction) {
        storage.transactions().getById(transaction.id());
        validateReferences(transaction, false);
        return commands.execute(new SaveCommand<>(storage.transactions(), transaction,
                describe("修改", transaction), Topic.TRANSACTIONS)).result();
    }

    public void delete(long id) {
        Transaction transaction = storage.transactions().getById(id);
        commands.execute(new DeleteCommand<>(storage.transactions(), id, describe("刪除", transaction),
                Topic.TRANSACTIONS));
    }

    /** 一次刪除多筆，復原時一起還原。 */
    public void deleteAll(Collection<Long> ids) {
        if (ids.isEmpty()) {
            return;
        }
        if (ids.size() == 1) {
            delete(ids.iterator().next());
            return;
        }
        commands.executeBatch("刪除 " + ids.size() + " 筆交易", batch -> {
            ids.forEach(id -> batch.run(new DeleteCommand<>(storage.transactions(), id, "刪除交易",
                    Topic.TRANSACTIONS)));
            return batch.size();
        });
    }

    /** 複製一筆交易到今天，常用於每天相同的早餐或通勤。 */
    public Transaction duplicateToToday(long id) {
        Transaction source = storage.transactions().getById(id);
        Transaction copy = source.toBuilder()
                .id(0)
                .date(LocalDate.now(clock))
                .time(LocalTime.now(clock))
                .recurringRuleId(null)
                .build();
        return add(copy);
    }

    public Transaction get(long id) {
        return storage.transactions().getById(id);
    }

    public List<Transaction> search(TransactionQuery query) {
        return storage.transactions().find(query);
    }

    public List<Transaction> recent(int limit) {
        return storage.transactions().findRecent(limit);
    }

    public long count() {
        return storage.transactions().count();
    }

    /** 最早一筆交易的日期。 */
    public Optional<LocalDate> earliestDate() {
        return storage.transactions().earliestDate();
    }

    /** 最近用過的備註，輸入框的自動完成清單。 */
    public List<String> noteSuggestions(int limit) {
        return storage.transactions().recentNotes(limit);
    }

    /**
     * 依過去相同備註的交易推薦分類，類似 HomeBank 的自動分類規則，但規則是從歷史學來的：
     * 先找備註完全相同的交易，沒有再找備註包含這段文字的交易，取最常用的分類。
     */
    public Optional<Long> suggestCategory(String note, TransactionType type) {
        String text = note == null ? "" : note.strip();
        if (text.isEmpty() || type == TransactionType.TRANSFER) {
            return Optional.empty();
        }
        List<Transaction> candidates = storage.transactions().find(TransactionQuery.all().withType(type).withText(text));
        String lower = text.toLowerCase(Locale.ROOT);
        List<Transaction> exact = candidates.stream()
                .filter(t -> t.note().toLowerCase(Locale.ROOT).equals(lower)).toList();
        List<Transaction> partial = candidates.stream()
                .filter(t -> t.note().toLowerCase(Locale.ROOT).contains(lower)).toList();
        List<Transaction> pool = exact.isEmpty() ? partial : exact;
        return pool.stream()
                .collect(Collectors.groupingBy(Transaction::categoryId, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.<Long, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder())))
                .map(Map.Entry::getKey);
    }

    /** 給畫面與狀態列使用的描述，例如「新增支出 餐飲 $120」。 */
    public String describe(String verb, Transaction transaction) {
        StringBuilder text = new StringBuilder(verb).append(transaction.type().displayName()).append(' ');
        if (transaction.isTransfer()) {
            text.append(walletName(transaction.walletId())).append(" → ").append(walletName(transaction.toWalletId()));
        } else {
            text.append(storage.categories().findById(transaction.categoryId()).map(Category::name).orElse("未分類"));
        }
        return text.append(' ').append(transaction.amount().format()).toString();
    }

    private String walletName(Long id) {
        return id == null ? "?" : storage.wallets().findById(id).map(Wallet::name).orElse("?");
    }

    private void validateReferences(Transaction transaction, boolean isNew) {
        Wallet wallet = storage.wallets().findById(transaction.walletId())
                .orElseThrow(() -> new ValidationException("錢包", "找不到選擇的錢包。"));
        if (isNew && wallet.archived()) {
            throw new ValidationException("錢包", "「" + wallet.name() + "」已封存，請選擇其他錢包。");
        }
        if (transaction.isTransfer()) {
            storage.wallets().findById(transaction.toWalletId())
                    .orElseThrow(() -> new ValidationException("轉入錢包", "找不到要轉入的錢包。"));
            return;
        }
        Category category = storage.categories().findById(transaction.categoryId())
                .orElseThrow(() -> new ValidationException("分類", "找不到選擇的分類。"));
        if (category.kind() != CategoryKind.of(transaction.type())) {
            throw new ValidationException("分類", "「" + category.name() + "」是" + category.kind().displayName()
                    + "分類，不能用在" + transaction.type().displayName() + "。");
        }
    }

    /** 依 id 建立錢包名稱對照表，畫面顯示用。 */
    public Map<Long, Wallet> walletsById() {
        return storage.wallets().findAll().stream().collect(Collectors.toMap(Wallet::id, Function.identity()));
    }
}
