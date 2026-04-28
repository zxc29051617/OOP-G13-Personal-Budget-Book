package persistence;

import java.util.ArrayList;
import java.util.List;

public class CsvUtil {
    private CsvUtil() {
    }

    public static String format(List<String> values) {
        List<String> escaped = new ArrayList<>();
        for (String value : values) {
            String safe = value == null ? "" : value;
            if (safe.contains(",") || safe.contains("\"") || safe.contains("\n")) {
                safe = "\"" + safe.replace("\"", "\"\"") + "\"";
            }
            escaped.add(safe);
        }
        return String.join(",", escaped);
    }

    public static List<String> parse(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());
        return values;
    }
}
