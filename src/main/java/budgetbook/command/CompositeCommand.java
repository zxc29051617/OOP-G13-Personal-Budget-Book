package budgetbook.command;

import budgetbook.event.Topic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Composite 模式：多個指令當成一個指令，例如匯入 CSV、定期交易一次入帳多筆。
 * 復原時以相反順序復原每個子指令。
 */
public final class CompositeCommand implements Command {
    private final String description;
    private final List<Command> children;

    public CompositeCommand(String description, List<? extends Command> children) {
        this.description = description;
        this.children = List.copyOf(children);
    }

    @Override
    public void execute() {
        children.forEach(Command::execute);
    }

    @Override
    public void undo() {
        List<Command> reversed = new ArrayList<>(children);
        Collections.reverse(reversed);
        reversed.forEach(Command::undo);
    }

    @Override
    public void redo() {
        children.forEach(Command::redo);
    }

    public int size() {
        return children.size();
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public Set<Topic> topics() {
        Set<Topic> topics = EnumSet.noneOf(Topic.class);
        children.forEach(child -> topics.addAll(child.topics()));
        return topics;
    }
}
