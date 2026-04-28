package ui;

import java.text.NumberFormat;
import java.util.Locale;

public class Money {
    private static final NumberFormat FORMAT = NumberFormat.getCurrencyInstance(Locale.TAIWAN);

    private Money() {
    }

    public static String format(double amount) {
        return FORMAT.format(amount);
    }
}
