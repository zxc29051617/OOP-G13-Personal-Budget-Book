package budgetbook.command;

import budgetbook.event.Topic;

import java.util.Set;

/**
 * Command 模式：把「使用者的一個操作」包成物件，記住足夠的資訊以便復原與重做。
 * 所有會修改資料的操作都經過 {@link CommandManager}，因此都可以 Ctrl+Z 復原。
 */
public interface Command {
    void execute();

    void undo();

    /** 預設重做就是再執行一次；新增資料的指令會改成以原本的 id 寫回，讓後續指令仍然有效。 */
    default void redo() {
        execute();
    }

    /** 給使用者看的描述，例如「新增支出 餐飲 $120」。 */
    String description();

    /** 這個操作影響哪些資料。 */
    Set<Topic> topics();
}
