package budgetbook.service;

import budgetbook.event.DataChangedEvent;
import budgetbook.event.EventBus;
import budgetbook.event.Topic;
import budgetbook.model.Checks;
import budgetbook.repository.SettingsRepository;

/** 使用者偏好設定（主題、種樹目標）。偏好不是記帳資料，所以不放進復原歷史。 */
public final class SettingsService {
    public static final String KEY_THEME = "ui.theme";
    public static final String KEY_TREE_GOAL = "tree.goal";
    public static final String KEY_WELCOMED = "app.welcomed";
    public static final int DEFAULT_TREE_GOAL = 20;

    private final SettingsRepository settings;
    private final EventBus bus;

    public SettingsService(SettingsRepository settings, EventBus bus) {
        this.settings = settings;
        this.bus = bus;
    }

    public boolean isDarkTheme() {
        return settings.get(KEY_THEME).map("dark"::equals).orElse(false);
    }

    public void setDarkTheme(boolean dark) {
        settings.put(KEY_THEME, dark ? "dark" : "light");
        bus.publish(DataChangedEvent.of(Topic.SETTINGS));
    }

    /** 每月要澆水幾次才算完成一棵樹，原版固定為 20。 */
    public int treeGoal() {
        try {
            return settings.get(KEY_TREE_GOAL).map(Integer::parseInt).orElse(DEFAULT_TREE_GOAL);
        } catch (NumberFormatException e) {
            return DEFAULT_TREE_GOAL;
        }
    }

    public void setTreeGoal(int goal) {
        Checks.requireRange(goal, 5, 100, "每月澆水目標");
        settings.put(KEY_TREE_GOAL, Integer.toString(goal));
        bus.publish(DataChangedEvent.of(Topic.SETTINGS));
    }

    public boolean isWelcomed() {
        return settings.get(KEY_WELCOMED).isPresent();
    }

    public void markWelcomed() {
        settings.put(KEY_WELCOMED, "true");
    }
}
