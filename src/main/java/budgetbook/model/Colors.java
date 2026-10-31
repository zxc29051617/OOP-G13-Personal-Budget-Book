package budgetbook.model;

import budgetbook.exception.ValidationException;

import java.util.List;
import java.util.Locale;

/** 分類、錢包與目標使用的顏色（以 0xRRGGBB 整數保存，model 不依賴 AWT）。 */
public final class Colors {
    /** 預設色盤，飽和度適中，淺色與深色主題都看得清楚。 */
    public static final List<Integer> PALETTE = List.of(
            0xE76F51, 0xF4A261, 0xE9C46A, 0x8AB17D, 0x2A9D8F, 0x4C9A62,
            0x457B9D, 0x5E60CE, 0x9B5DE5, 0xD16BA5, 0xB5838D, 0x6C757D);

    private Colors() {
    }

    public static int paletteColor(int index) {
        return PALETTE.get(Math.floorMod(index, PALETTE.size()));
    }

    public static String toHex(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    public static int parseHex(String hex) {
        String value = hex == null ? "" : hex.strip();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (!value.matches("[0-9a-fA-F]{6}")) {
            throw new ValidationException("顏色", "顏色格式必須是 #RRGGBB：" + hex);
        }
        return Integer.parseInt(value, 16);
    }
}
