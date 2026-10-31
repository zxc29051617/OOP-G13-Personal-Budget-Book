package budgetbook.event;

/** 復原／重做的狀態改變，選單依此啟用或停用。 */
public record HistoryChangedEvent(boolean canUndo, boolean canRedo, String undoLabel, String redoLabel) {
}
