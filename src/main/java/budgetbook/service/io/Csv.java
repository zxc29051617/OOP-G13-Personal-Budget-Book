package budgetbook.service.io;

import budgetbook.exception.CsvFormatException;

import java.util.ArrayList;
import java.util.List;

/**
 * RFC 4180 CSV 讀寫：支援雙引號、欄位內的逗號與換行、CRLF，並略過 UTF-8 BOM。
 */
public final class Csv {
    private Csv() {
    }

    /** 一列資料與它在檔案中的起始行號。 */
    public record Row(int line, List<String> values) {
        public String get(int index) {
            return index >= 0 && index < values.size() ? values.get(index) : "";
        }
    }

    public static String escape(String value) {
        String text = value == null ? "" : value;
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    public static String row(List<String> values) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                line.append(',');
            }
            line.append(escape(values.get(i)));
        }
        return line.toString();
    }

    /**
     * 防止 CSV 公式注入：以 = + - @ 開頭的文字在 Excel 中可能被當成公式執行，
     * 匯出時在前面加上單引號；匯入時再去掉。
     */
    public static String protectFormula(String text) {
        if (text != null && !text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) {
            return "'" + text;
        }
        return text == null ? "" : text;
    }

    public static String unprotectFormula(String text) {
        if (text != null && text.length() > 1 && text.charAt(0) == '\'' && "=+-@".indexOf(text.charAt(1)) >= 0) {
            return text.substring(1);
        }
        return text == null ? "" : text;
    }

    public static List<Row> parse(String content) {
        String text = content.startsWith("﻿") ? content.substring(1) : content;
        List<Row> rows = new ArrayList<>();
        List<String> values = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean fieldStarted = false;
        int line = 1;
        int rowStart = 1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        quoted = false;
                    }
                } else {
                    if (c == '\n') {
                        line++;
                    }
                    field.append(c);
                }
            } else if (c == '"' && field.isEmpty() && !fieldStarted) {
                quoted = true;
                fieldStarted = true;
            } else if (c == ',') {
                values.add(field.toString());
                field.setLength(0);
                fieldStarted = false;
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                values.add(field.toString());
                addRow(rows, rowStart, values);
                values = new ArrayList<>();
                field.setLength(0);
                fieldStarted = false;
                line++;
                rowStart = line;
            } else {
                field.append(c);
                fieldStarted = true;
            }
        }
        if (quoted) {
            throw new CsvFormatException(rowStart, "引號沒有成對。");
        }
        if (fieldStarted || !field.isEmpty() || !values.isEmpty()) {
            values.add(field.toString());
            addRow(rows, rowStart, values);
        }
        return rows;
    }

    private static void addRow(List<Row> rows, int line, List<String> values) {
        boolean blank = values.stream().allMatch(String::isBlank);
        if (!blank) {
            rows.add(new Row(line, List.copyOf(values)));
        }
    }
}
