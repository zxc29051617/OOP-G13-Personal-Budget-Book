package budgetbook.service;

import budgetbook.command.CommandManager;
import budgetbook.command.DeleteCommand;
import budgetbook.command.SaveCommand;
import budgetbook.event.Topic;
import budgetbook.exception.ValidationException;
import budgetbook.model.Category;
import budgetbook.model.CategoryKind;
import budgetbook.model.RecurringRule;
import budgetbook.model.Transaction;
import budgetbook.repository.Storage;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 定期交易：啟動時自動補入帳到今天為止到期的交易（類似 GnuCash 的 Since Last Run），
 * 也可以手動提前入帳或略過一次（類似 Actual Budget 的 schedules）。
 */
public final class RecurringService {
    private final Storage storage;
    private final CommandManager commands;
    private final Clock clock;

    public RecurringService(Storage storage, CommandManager commands, Clock clock) {
        this.storage = storage;
        this.commands = commands;
        this.clock = clock;
    }

    /** 一次預定的交易。 */
    public record Occurrence(RecurringRule rule, LocalDate date) {
    }

    public List<RecurringRule> all() {
        return storage.recurringRules().findAll();
    }

    public RecurringRule get(long id) {
        return storage.recurringRules().getById(id);
    }

    public RecurringRule save(RecurringRule rule) {
        validateReferences(rule);
        String verb = rule.isNew() ? "新增" : "修改";
        return commands.execute(new SaveCommand<>(storage.recurringRules(), rule,
                verb + "定期交易「" + rule.name() + "」", Topic.RECURRING)).result();
    }

    public void delete(long id) {
        RecurringRule rule = get(id);
        commands.execute(new DeleteCommand<>(storage.recurringRules(), id, "刪除定期交易「" + rule.name() + "」",
                Topic.RECURRING));
    }

    public RecurringRule setActive(long id, boolean active) {
        RecurringRule rule = get(id).withActive(active);
        return commands.execute(new SaveCommand<>(storage.recurringRules(), rule,
                (active ? "啟用" : "暫停") + "定期交易「" + rule.name() + "」", Topic.RECURRING)).result();
    }

    /** 今天以前（含）應入帳但尚未入帳的次數。 */
    public int dueCount() {
        LocalDate today = LocalDate.now(clock);
        return all().stream().mapToInt(rule -> rule.dueDates(today).size()).sum();
    }

    /** 把所有到期的定期交易入帳，整批可以一次復原。 */
    public List<Transaction> postDue() {
        LocalDate today = LocalDate.now(clock);
        List<RecurringRule> rules = all();
        int total = rules.stream().mapToInt(rule -> rule.dueDates(today).size()).sum();
        if (total == 0) {
            return List.of();
        }
        return commands.executeBatch("定期交易自動入帳 " + total + " 筆", batch -> {
            List<Transaction> posted = new ArrayList<>();
            for (RecurringRule rule : rules) {
                List<LocalDate> dates = rule.dueDates(today);
                for (LocalDate date : dates) {
                    posted.add(batch.run(new SaveCommand<>(storage.transactions(), rule.toTransaction(date),
                            "定期交易入帳", Topic.TRANSACTIONS)).result());
                }
                if (!dates.isEmpty()) {
                    batch.run(new SaveCommand<>(storage.recurringRules(),
                            rule.withPostedCount(rule.postedCount() + dates.size()), "更新定期交易", Topic.RECURRING));
                }
            }
            return posted;
        });
    }

    /** 不等到期日，立即入帳下一次（例如提早繳房租）。 */
    public Transaction postNext(long id) {
        RecurringRule rule = get(id);
        LocalDate date = rule.nextDueDate()
                .orElseThrow(() -> new ValidationException("定期交易", "「" + rule.name() + "」已經結束。"));
        return commands.executeBatch("入帳定期交易「" + rule.name() + "」", batch -> {
            Transaction posted = batch.run(new SaveCommand<>(storage.transactions(), rule.toTransaction(date),
                    "定期交易入帳", Topic.TRANSACTIONS)).result();
            batch.run(new SaveCommand<>(storage.recurringRules(), rule.withPostedCount(rule.postedCount() + 1),
                    "更新定期交易", Topic.RECURRING));
            return posted;
        });
    }

    /** 略過下一次，不產生交易。 */
    public void skipNext(long id) {
        RecurringRule rule = get(id);
        if (rule.isFinished()) {
            throw new ValidationException("定期交易", "「" + rule.name() + "」已經結束。");
        }
        commands.execute(new SaveCommand<>(storage.recurringRules(), rule.withPostedCount(rule.postedCount() + 1),
                "略過一次「" + rule.name() + "」", Topic.RECURRING));
    }

    /** 今天起 {@code days} 天內預定的交易（含逾期未入帳），依日期排序。 */
    public List<Occurrence> upcoming(int days) {
        LocalDate until = LocalDate.now(clock).plusDays(days);
        List<Occurrence> result = new ArrayList<>();
        for (RecurringRule rule : all()) {
            rule.upcomingUntil(until, 62).forEach(date -> result.add(new Occurrence(rule, date)));
        }
        result.sort(Comparator.comparing(Occurrence::date).thenComparing(o -> o.rule().name()));
        return result;
    }

    private void validateReferences(RecurringRule rule) {
        storage.wallets().findById(rule.walletId())
                .orElseThrow(() -> new ValidationException("錢包", "找不到選擇的錢包。"));
        if (rule.toWalletId() != null) {
            storage.wallets().findById(rule.toWalletId())
                    .orElseThrow(() -> new ValidationException("轉入錢包", "找不到要轉入的錢包。"));
        }
        if (rule.categoryId() != null) {
            Category category = storage.categories().findById(rule.categoryId())
                    .orElseThrow(() -> new ValidationException("分類", "找不到選擇的分類。"));
            if (category.kind() != CategoryKind.of(rule.type())) {
                throw new ValidationException("分類", "「" + category.name() + "」不是" + rule.type().displayName() + "分類。");
            }
        }
    }
}
