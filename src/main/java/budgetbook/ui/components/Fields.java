package budgetbook.ui.components;

import budgetbook.exception.ValidationException;
import budgetbook.model.AmountExpression;
import budgetbook.model.Money;
import com.formdev.flatlaf.FlatClientProperties;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javax.swing.JTextField;

/** 日期、時間與金額輸入框。上下方向鍵可以調整日期與時間。 */
public final class Fields {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private Fields() {
    }

    /** 日期欄位：yyyy-MM-dd；↑↓ 加減一天，PageUp/PageDown 加減一個月。 */
    public static final class DateField extends JTextField {
        public DateField(LocalDate initial) {
            super(initial == null ? "" : initial.toString(), 10);
            putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "2026-10-31");
            setToolTipText("格式 yyyy-MM-dd；↑↓ 加減一天，PageUp／PageDown 加減一個月");
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    int days = switch (e.getKeyCode()) {
                        case KeyEvent.VK_UP -> 1;
                        case KeyEvent.VK_DOWN -> -1;
                        default -> 0;
                    };
                    int months = switch (e.getKeyCode()) {
                        case KeyEvent.VK_PAGE_UP -> 1;
                        case KeyEvent.VK_PAGE_DOWN -> -1;
                        default -> 0;
                    };
                    if (days != 0 || months != 0) {
                        LocalDate date = parseOrNull();
                        if (date != null) {
                            setDate(date.plusDays(days).plusMonths(months));
                            e.consume();
                        }
                    }
                }
            });
        }

        public void setDate(LocalDate date) {
            setText(date == null ? "" : date.toString());
            putClientProperty(FlatClientProperties.OUTLINE, null);
        }

        public LocalDate date(String field) {
            LocalDate date = parseOrNull();
            if (date == null) {
                putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
                throw new ValidationException(field, field + "格式不正確，請輸入像 2026-10-31 的日期。");
            }
            putClientProperty(FlatClientProperties.OUTLINE, null);
            return date;
        }

        /** 可以空白的日期欄位（例如結束日期）。 */
        public LocalDate optionalDate(String field) {
            return getText().isBlank() ? null : date(field);
        }

        private LocalDate parseOrNull() {
            try {
                return LocalDate.parse(getText().strip().replace('/', '-'));
            } catch (DateTimeParseException e) {
                return null;
            }
        }
    }

    /** 時間欄位：HH:mm；↑↓ 加減五分鐘。 */
    public static final class TimeField extends JTextField {
        public TimeField(LocalTime initial) {
            super(initial == null ? "" : initial.format(TIME), 5);
            putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "12:30");
            setToolTipText("格式 HH:mm；↑↓ 加減五分鐘");
            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    int minutes = e.getKeyCode() == KeyEvent.VK_UP ? 5 : e.getKeyCode() == KeyEvent.VK_DOWN ? -5 : 0;
                    if (minutes != 0) {
                        try {
                            setTime(LocalTime.parse(getText().strip()).plusMinutes(minutes));
                            e.consume();
                        } catch (DateTimeParseException ignored) {
                            // 格式錯誤時不調整，儲存時會提示
                        }
                    }
                }
            });
        }

        public void setTime(LocalTime time) {
            setText(time.format(TIME));
        }

        public LocalTime time() {
            String text = getText().strip();
            if (text.isEmpty()) {
                return LocalTime.MIDNIGHT;
            }
            try {
                return LocalTime.parse(text.length() == 4 ? "0" + text : text);
            } catch (DateTimeParseException e) {
                putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
                throw new ValidationException("時間", "時間格式不正確，請輸入像 12:30 的時間。");
            }
        }
    }

    /**
     * 金額欄位：可以輸入算式，例如 120+45，離開欄位時自動算出結果。
     * 不加 ActionListener，讓 Enter 直接觸發對話框的「儲存」。
     */
    public static final class AmountField extends JTextField {
        public AmountField(Money initial) {
            super(initial == null ? "" : initial.toPlainString(), 10);
            putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "可輸入算式，例如 120+45");
            setHorizontalAlignment(RIGHT);
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    evaluateQuietly();
                }
            });
        }

        public void setAmount(Money amount) {
            setText(amount == null ? "" : amount.toPlainString());
        }

        public Money amount() {
            try {
                Money value = AmountExpression.evaluate(getText());
                putClientProperty(FlatClientProperties.OUTLINE, null);
                return value;
            } catch (ValidationException e) {
                putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
                throw e;
            }
        }

        private void evaluateQuietly() {
            String text = getText();
            if (text.matches(".*\\d.*[-+*/×÷()].*")) {
                try {
                    setText(AmountExpression.evaluate(text).toPlainString());
                    putClientProperty(FlatClientProperties.OUTLINE, null);
                } catch (ValidationException ignored) {
                    putClientProperty(FlatClientProperties.OUTLINE, FlatClientProperties.OUTLINE_ERROR);
                }
            }
        }
    }
}
