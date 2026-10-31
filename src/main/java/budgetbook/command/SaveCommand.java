package budgetbook.command;

import budgetbook.event.Topic;
import budgetbook.model.Entity;
import budgetbook.repository.Repository;

import java.util.Arrays;
import java.util.Set;

/**
 * 新增或修改一筆資料。修改時先記下舊版本，復原時寫回；新增的資料復原時刪除，
 * 重做時以原本的 id 還原，所以之後針對這個 id 的指令也能正確重做。
 */
public final class SaveCommand<T extends Entity> implements Command {
    private final Repository<T> repository;
    private final T entity;
    private final String description;
    private final Set<Topic> topics;
    private T saved;
    private T previous;

    public SaveCommand(Repository<T> repository, T entity, String description, Topic... topics) {
        this.repository = repository;
        this.entity = entity;
        this.description = description;
        this.topics = Set.copyOf(Arrays.asList(topics));
    }

    @Override
    public void execute() {
        if (entity.isNew()) {
            previous = null;
            saved = repository.save(entity);
        } else {
            previous = repository.getById(entity.id());
            saved = repository.save(entity);
        }
    }

    @Override
    public void undo() {
        if (previous == null) {
            repository.deleteById(saved.id());
        } else {
            repository.save(previous);
        }
    }

    @Override
    public void redo() {
        if (previous == null) {
            repository.restore(saved);
        } else {
            repository.save(saved);
        }
    }

    /** 執行後的資料（新增時帶有資料庫給的 id）。 */
    public T result() {
        if (saved == null) {
            throw new IllegalStateException("Command has not been executed");
        }
        return saved;
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
