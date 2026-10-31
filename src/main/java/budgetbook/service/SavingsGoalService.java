package budgetbook.service;

import budgetbook.command.CommandManager;
import budgetbook.command.DeleteCommand;
import budgetbook.command.SaveCommand;
import budgetbook.event.Topic;
import budgetbook.model.Money;
import budgetbook.model.SavingsGoal;
import budgetbook.repository.Storage;

import java.util.List;

/**
 * 儲蓄目標。存入／取出只是標記「這筆錢是為了這個目標存的」，錢仍然在原本的錢包裡，
 * 所以不會產生交易，也不影響錢包餘額。
 */
public final class SavingsGoalService {
    private final Storage storage;
    private final CommandManager commands;

    public SavingsGoalService(Storage storage, CommandManager commands) {
        this.storage = storage;
        this.commands = commands;
    }

    public List<SavingsGoal> all() {
        return storage.savingsGoals().findAll();
    }

    public SavingsGoal get(long id) {
        return storage.savingsGoals().getById(id);
    }

    public SavingsGoal save(SavingsGoal goal) {
        String verb = goal.isNew() ? "新增" : "修改";
        return commands.execute(new SaveCommand<>(storage.savingsGoals(), goal,
                verb + "儲蓄目標「" + goal.name() + "」", Topic.GOALS)).result();
    }

    public void delete(long id) {
        SavingsGoal goal = get(id);
        commands.execute(new DeleteCommand<>(storage.savingsGoals(), id, "刪除儲蓄目標「" + goal.name() + "」",
                Topic.GOALS));
    }

    public SavingsGoal deposit(long id, Money amount) {
        SavingsGoal goal = get(id).deposit(amount);
        return commands.execute(new SaveCommand<>(storage.savingsGoals(), goal,
                "存入「" + goal.name() + "」" + amount.format(), Topic.GOALS)).result();
    }

    public SavingsGoal withdraw(long id, Money amount) {
        SavingsGoal goal = get(id).withdraw(amount);
        return commands.execute(new SaveCommand<>(storage.savingsGoals(), goal,
                "從「" + goal.name() + "」取出 " + amount.format(), Topic.GOALS)).result();
    }

    public Money totalSaved() {
        return Money.sum(all().stream().map(SavingsGoal::saved));
    }
}
