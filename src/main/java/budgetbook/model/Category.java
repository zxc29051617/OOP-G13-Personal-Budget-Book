package budgetbook.model;

/**
 * 收支分類，例如「餐飲」「薪資」。使用者可以自訂名稱、顏色與圖示文字。
 *
 * @param color 0xRRGGBB
 * @param icon  圖示文字，1 到 2 個字（中文字、符號或 emoji），空白時取名稱第一個字
 */
public record Category(long id, String name, CategoryKind kind, int color, String icon) implements Entity {
    public static final int MAX_NAME_LENGTH = 20;

    public Category {
        name = Checks.requireText(name, "分類名稱", MAX_NAME_LENGTH);
        Checks.requireValue(kind, "分類類型");
        color = color & 0xFFFFFF;
        icon = normalizeIcon(icon, name);
    }

    public static Category create(String name, CategoryKind kind, int color, String icon) {
        return new Category(0, name, kind, color, icon);
    }

    public Category withId(long newId) {
        return new Category(newId, name, kind, color, icon);
    }

    public Category withName(String newName) {
        return new Category(id, newName, kind, color, icon);
    }

    public Category withColor(int newColor) {
        return new Category(id, name, kind, newColor, icon);
    }

    public Category withIcon(String newIcon) {
        return new Category(id, name, kind, color, newIcon);
    }

    public String colorHex() {
        return Colors.toHex(color);
    }

    static String normalizeIcon(String icon, String name) {
        String value = icon == null ? "" : icon.strip();
        if (value.isEmpty()) {
            // 預設只取名稱的第一個字，像原本版本的圓形徽章
            return name.substring(0, name.offsetByCodePoints(0, 1));
        }
        int count = Math.min(2, value.codePointCount(0, value.length()));
        return value.substring(0, value.offsetByCodePoints(0, count));
    }

    @Override
    public String toString() {
        return name;
    }
}
