package budgetbook.service;

import budgetbook.command.CommandManager;
import budgetbook.command.DeleteCommand;
import budgetbook.command.SaveCommand;
import budgetbook.event.Topic;
import budgetbook.exception.EntityInUseException;
import budgetbook.exception.ValidationException;
import budgetbook.model.Budget;
import budgetbook.model.Category;
import budgetbook.model.CategoryKind;
import budgetbook.model.RecurringRule;
import budgetbook.model.Transaction;
import budgetbook.model.TransactionQuery;
import budgetbook.repository.Storage;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 自訂分類（名稱、顏色、圖示）的新增、修改、刪除與合併。 */
public final class CategoryService {
    private final Storage storage;
    private final CommandManager commands;

    public CategoryService(Storage storage, CommandManager commands) {
        this.storage = storage;
        this.commands = commands;
    }

    public List<Category> all() {
        return storage.categories().findAll();
    }

    public List<Category> byKind(CategoryKind kind) {
        return storage.categories().findByKind(kind);
    }

    public Category get(long id) {
        return storage.categories().getById(id);
    }

    public Optional<Category> find(Long id) {
        return id == null ? Optional.empty() : storage.categories().findById(id);
    }

    public Map<Long, Category> byId() {
        return all().stream().collect(Collectors.toMap(Category::id, Function.identity()));
    }

    public Category create(String name, CategoryKind kind, int color, String icon) {
        Category category = Category.create(name, kind, color, icon);
        ensureUniqueName(category);
        return commands.execute(new SaveCommand<>(storage.categories(), category,
                "新增分類「" + category.name() + "」", Topic.CATEGORIES)).result();
    }

    public Category update(Category category) {
        Category existing = get(category.id());
        if (existing.kind() != category.kind() && usageCount(category.id()) > 0) {
            throw new ValidationException("分類類型", "「" + existing.name() + "」已經有交易，不能改成"
                    + category.kind().displayName() + "分類。");
        }
        ensureUniqueName(category);
        return commands.execute(new SaveCommand<>(storage.categories(), category,
                "修改分類「" + category.name() + "」", Topic.CATEGORIES, Topic.TRANSACTIONS, Topic.BUDGETS))
                .result();
    }

    /** 交易與定期交易使用這個分類的次數。 */
    public long usageCount(long categoryId) {
        return storage.transactions().countByCategory(categoryId) + storage.recurringRules().countByCategory(categoryId);
    }

    /** 刪除沒有被使用的分類；它的預算一起刪除（可復原）。 */
    public void delete(long id) {
        Category category = get(id);
        long used = usageCount(id);
        if (used > 0) {
            throw new EntityInUseException("「" + category.name() + "」還有 " + used
                    + " 筆交易或定期交易使用，請先用「合併到其他分類」移走再刪除。");
        }
        commands.executeBatch("刪除分類「" + category.name() + "」", batch -> {
            deleteBudgetOf(id, batch);
            return batch.run(new DeleteCommand<>(storage.categories(), id, "刪除分類", Topic.CATEGORIES));
        });
    }

    /**
     * 把 {@code sourceId} 的交易、定期交易都改到 {@code targetId}，再刪除來源分類。
     *
     * @return 移動的交易筆數
     */
    public int mergeInto(long sourceId, long targetId) {
        Category source = get(sourceId);
        Category target = get(targetId);
        if (sourceId == targetId) {
            throw new ValidationException("分類", "請選擇另一個分類。");
        }
        if (source.kind() != target.kind()) {
            throw new ValidationException("分類", "只能合併到同樣是" + source.kind().displayName() + "的分類。");
        }
        List<Transaction> moved = storage.transactions().find(TransactionQuery.all().withCategory(sourceId));
        List<RecurringRule> rules = storage.recurringRules().findAll().stream()
                .filter(rule -> rule.categoryId() != null && rule.categoryId() == sourceId).toList();
        commands.executeBatch("把「" + source.name() + "」合併到「" + target.name() + "」", batch -> {
            for (Transaction transaction : moved) {
                batch.run(new SaveCommand<>(storage.transactions(),
                        transaction.toBuilder().categoryId(targetId).build(), "移動交易", Topic.TRANSACTIONS));
            }
            for (RecurringRule rule : rules) {
                RecurringRule updated = new RecurringRule(rule.id(), rule.name(), rule.type(), rule.amount(), targetId,
                        rule.walletId(), rule.toWalletId(), rule.note(), rule.frequency(), rule.interval(),
                        rule.startDate(), rule.endDate(), rule.postedCount(), rule.active());
                batch.run(new SaveCommand<>(storage.recurringRules(), updated, "移動定期交易", Topic.RECURRING));
            }
            deleteBudgetOf(sourceId, batch);
            return batch.run(new DeleteCommand<>(storage.categories(), sourceId, "刪除分類", Topic.CATEGORIES,
                    Topic.TRANSACTIONS));
        });
        return moved.size();
    }

    private void deleteBudgetOf(long categoryId, CommandManager.Batch batch) {
        Optional<Budget> budget = storage.budgets().findByCategoryId(categoryId);
        budget.ifPresent(b -> batch.run(new DeleteCommand<>(storage.budgets(), b.id(), "刪除預算", Topic.BUDGETS)));
    }

    private void ensureUniqueName(Category category) {
        storage.categories().findByName(category.kind(), category.name())
                .filter(other -> other.id() != category.id())
                .ifPresent(other -> {
                    throw new ValidationException("分類名稱", "已經有名為「" + other.name() + "」的"
                            + other.kind().displayName() + "分類。");
                });
    }
}
