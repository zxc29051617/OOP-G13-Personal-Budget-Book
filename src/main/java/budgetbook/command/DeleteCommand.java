package budgetbook.command;

import budgetbook.event.Topic;
import budgetbook.model.Entity;
import budgetbook.repository.Repository;

import java.util.Arrays;
import java.util.Set;

/** 刪除一筆資料；執行前先保存整筆資料，復原時以原本的 id 寫回。 */
public final class DeleteCommand<T extends Entity> implements Command {
    private final Repository<T> repository;
    private final long id;
    private final String description;
    private final Set<Topic> topics;
    private T removed;

    public DeleteCommand(Repository<T> repository, long id, String description, Topic... topics) {
        this.repository = repository;
        this.id = id;
        this.description = description;
        this.topics = Set.copyOf(Arrays.asList(topics));
    }

    @Override
    public void execute() {
        removed = repository.getById(id);
        repository.deleteById(id);
    }

    @Override
    public void undo() {
        repository.restore(removed);
    }

    @Override
    public void redo() {
        repository.deleteById(id);
    }

    public T removed() {
        return removed;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public Set<Topic> topics() {
        return topics;
    }
}
