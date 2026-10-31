package budgetbook.event;

/** 一個指令被執行、復原或重做，狀態列會顯示這個訊息。 */
public record CommandEvent(String description, Kind kind) {
    public enum Kind {
        EXECUTED,
        UNDONE,
        REDONE
    }

    public String message() {
        return switch (kind) {
            case EXECUTED -> description;
            case UNDONE -> "已復原：" + description;
            case REDONE -> "已重做：" + description;
        };
    }
}
