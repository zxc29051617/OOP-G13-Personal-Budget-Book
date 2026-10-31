package budgetbook.ui;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.InputEvent;

/** 快捷鍵修飾鍵：Windows／Linux 為 Ctrl，macOS 為 Command。 */
public final class Keys {
    private Keys() {
    }

    public static int menu() {
        return GraphicsEnvironment.isHeadless() ? InputEvent.CTRL_DOWN_MASK
                : Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    }

    public static int shiftMenu() {
        return menu() | InputEvent.SHIFT_DOWN_MASK;
    }
}
