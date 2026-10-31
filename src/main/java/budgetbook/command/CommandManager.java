package budgetbook.command;

import budgetbook.event.CommandEvent;
import budgetbook.event.DataChangedEvent;
import budgetbook.event.EventBus;
import budgetbook.event.HistoryChangedEvent;
import budgetbook.repository.Storage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Command 模式中的 Invoker：執行指令並保存復原／重做歷史。
 *
 * <p>每個指令都在一個資料庫交易中執行，失敗時資料完全不變；成功後發布
 * {@link DataChangedEvent} 讓畫面更新。</p>
 */
public final class CommandManager {
    public static final int DEFAULT_HISTORY_LIMIT = 100;

    private final Storage storage;
    private final EventBus bus;
    private final int historyLimit;
    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();

    public CommandManager(Storage storage, EventBus bus) {
        this(storage, bus, DEFAULT_HISTORY_LIMIT);
    }

    public CommandManager(Storage storage, EventBus bus, int historyLimit) {
        this.storage = storage;
        this.bus = bus;
        this.historyLimit = historyLimit;
    }

    /** 一次批次中依序執行的指令；後面的指令可以使用前面指令的結果（例如新分類的 id）。 */
    public static final class Batch {
        private final List<Command> executed = new ArrayList<>();

        public <C extends Command> C run(C command) {
            command.execute();
            executed.add(command);
            return command;
        }

        public int size() {
            return executed.size();
        }
    }

    public <C extends Command> C execute(C command) {
        storage.inTransaction(command::execute);
        record(command);
        notifyChange(command, CommandEvent.Kind.EXECUTED);
        return command;
    }

    /** 在同一個資料庫交易中執行多個指令，並在歷史中合併成一個可復原的步驟。 */
    public <R> R executeBatch(String description, Function<Batch, R> work) {
        Batch batch = new Batch();
        R result = storage.inTransaction(() -> work.apply(batch));
        if (!batch.executed.isEmpty()) {
            CompositeCommand composite = new CompositeCommand(description, batch.executed);
            record(composite);
            notifyChange(composite, CommandEvent.Kind.EXECUTED);
        }
        return result;
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public Optional<String> undoDescription() {
        return Optional.ofNullable(undoStack.peek()).map(Command::description);
    }

    public Optional<String> redoDescription() {
        return Optional.ofNullable(redoStack.peek()).map(Command::description);
    }

    /** 復原最近一個指令；失敗時指令留在歷史中，資料維持原狀。 */
    public void undo() {
        Command command = undoStack.peek();
        if (command == null) {
            return;
        }
        storage.inTransaction(command::undo);
        undoStack.pop();
        redoStack.push(command);
        notifyChange(command, CommandEvent.Kind.UNDONE);
    }

    public void redo() {
        Command command = redoStack.peek();
        if (command == null) {
            return;
        }
        storage.inTransaction(command::redo);
        redoStack.pop();
        undoStack.push(command);
        notifyChange(command, CommandEvent.Kind.REDONE);
    }

    /** 清除歷史，例如重新載入示範資料後舊的歷史已不適用。 */
    public void clearHistory() {
        undoStack.clear();
        redoStack.clear();
        bus.publish(historyEvent());
    }

    public int undoSize() {
        return undoStack.size();
    }

    private void record(Command command) {
        undoStack.push(command);
        while (undoStack.size() > historyLimit) {
            undoStack.removeLast();
        }
        redoStack.clear();
    }

    private void notifyChange(Command command, CommandEvent.Kind kind) {
        bus.publish(new DataChangedEvent(command.topics()));
        bus.publish(new CommandEvent(command.description(), kind));
        bus.publish(historyEvent());
    }

    private HistoryChangedEvent historyEvent() {
        return new HistoryChangedEvent(canUndo(), canRedo(), undoDescription().orElse(""),
                redoDescription().orElse(""));
    }
}
