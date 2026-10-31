package budgetbook.service.io;

import budgetbook.command.CommandManager;
import budgetbook.command.SaveCommand;
import budgetbook.event.Topic;
import budgetbook.exception.BudgetBookException;
import budgetbook.exception.CsvFormatException;
import budgetbook.model.Category;
import budgetbook.model.CategoryKind;
import budgetbook.model.Checks;
import budgetbook.model.Colors;
import budgetbook.model.DateRange;
import budgetbook.model.Money;
import budgetbook.model.Transaction;
import budgetbook.model.TransactionType;
import budgetbook.model.Wallet;
import budgetbook.model.WalletType;
import budgetbook.repository.Storage;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 交易的 CSV 匯出與匯入。
 *
 * <p>匯出使用 UTF-8（含 BOM，Excel 可以直接開啟中文）。匯入時欄位順序不限，標題可以是中文或英文；
 * 不存在的錢包與分類會自動建立，與資料庫中完全相同的交易視為重複而略過。整批匯入可以一次復原。</p>
 */
public final class TransactionCsvService {
    public static final List<String> HEADER = List.of("日期", "時間", "類型", "金額", "分類", "錢包", "轉入錢包", "備註");
    private static final Map<String, String> HEADER_ALIASES = Map.ofEntries(
            Map.entry("date", "日期"), Map.entry("time", "時間"), Map.entry("type", "類型"),
            Map.entry("amount", "金額"), Map.entry("category", "分類"), Map.entry("wallet", "錢包"),
            Map.entry("account", "錢包"), Map.entry("to_wallet", "轉入錢包"), Map.entry("towallet", "轉入錢包"),
            Map.entry("note", "備註"), Map.entry("memo", "備註"), Map.entry("description", "備註"),
            Map.entry("帳戶", "錢包"), Map.entry("說明", "備註"));
    private static final DateTimeFormatter SLASH_DATE = DateTimeFormatter.ofPattern("yyyy/M/d");
    private static final DateTimeFormatter DASH_DATE = DateTimeFormatter.ofPattern("yyyy-M-d");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final Storage storage;
    private final CommandManager commands;

    public TransactionCsvService(Storage storage, CommandManager commands) {
        this.storage = storage;
        this.commands = commands;
    }

    public void export(List<Transaction> transactions, Writer out) throws IOException {
        Map<Long, String> walletNames = new HashMap<>();
        storage.wallets().findAll().forEach(w -> walletNames.put(w.id(), w.name()));
        Map<Long, String> categoryNames = new HashMap<>();
        storage.categories().findAll().forEach(c -> categoryNames.put(c.id(), c.name()));

        out.write('﻿');
        out.write(Csv.row(HEADER));
        out.write("\r\n");
        for (Transaction t : transactions) {
            out.write(Csv.row(List.of(
                    t.date().toString(),
                    t.time().format(TIME),
                    t.type().displayName(),
                    t.amount().toPlainString(),
                    Csv.protectFormula(t.categoryId() == null ? "" : categoryNames.getOrDefault(t.categoryId(), "")),
                    Csv.protectFormula(walletNames.getOrDefault(t.walletId(), "")),
                    Csv.protectFormula(t.toWalletId() == null ? "" : walletNames.getOrDefault(t.toWalletId(), "")),
                    Csv.protectFormula(t.note()))));
            out.write("\r\n");
        }
        out.flush();
    }

    public String exportToString(List<Transaction> transactions) {
        StringWriter writer = new StringWriter();
        try {
            export(transactions, writer);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return writer.toString();
    }

    /** 解析 CSV 並標記重複的列，不寫入資料庫。 */
    public ImportPreview preview(Reader in) throws IOException {
        List<Csv.Row> rows = Csv.parse(readAll(in));
        if (rows.isEmpty()) {
            throw new CsvFormatException(1, "檔案是空的。");
        }
        Map<String, Integer> columns = mapHeader(rows.get(0));
        List<ImportRow> parsed = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (Csv.Row row : rows.subList(1, rows.size())) {
            try {
                parsed.add(parseRow(row, columns));
            } catch (BudgetBookException e) {
                errors.add("第 " + row.line() + " 行：" + e.getMessage());
            }
        }
        return new ImportPreview(markDuplicates(parsed), errors);
    }

    /** 匯入預覽中不重複的列。 */
    public ImportResult importRows(ImportPreview preview) {
        List<ImportRow> fresh = preview.rows().stream().filter(row -> !row.duplicate()).toList();
        int duplicates = (int) preview.duplicateCount();
        if (fresh.isEmpty()) {
            return new ImportResult(0, duplicates, 0, 0, preview.errors());
        }
        int[] created = new int[2];
        commands.executeBatch("匯入 CSV " + fresh.size() + " 筆交易", batch -> {
            Map<String, Long> wallets = new HashMap<>();
            storage.wallets().findAll().forEach(w -> wallets.put(w.name(), w.id()));
            Map<String, Long> categories = new HashMap<>();
            storage.categories().findAll().forEach(c -> categories.put(c.kind() + ":" + c.name(), c.id()));
            for (ImportRow row : fresh) {
                long walletId = walletId(row.wallet(), wallets, batch, created);
                Long toWalletId = row.type() == TransactionType.TRANSFER
                        ? walletId(row.toWallet(), wallets, batch, created) : null;
                Long categoryId = row.type() == TransactionType.TRANSFER ? null
                        : categoryId(row.category(), CategoryKind.of(row.type()), categories, batch, created);
                Transaction transaction = Transaction.builder().type(row.type()).date(row.date()).time(row.time())
                        .amount(row.amount()).categoryId(categoryId).walletId(walletId).toWalletId(toWalletId)
                        .note(row.note()).build();
                batch.run(new SaveCommand<>(storage.transactions(), transaction, "匯入交易", Topic.TRANSACTIONS));
            }
            return null;
        });
        return new ImportResult(fresh.size(), duplicates, created[0], created[1], preview.errors());
    }

    public ImportResult importCsv(Reader in) throws IOException {
        return importRows(preview(in));
    }

    private long walletId(String name, Map<String, Long> wallets, CommandManager.Batch batch, int[] created) {
        Long id = wallets.get(name);
        if (id == null) {
            Wallet wallet = Wallet.create(name, WalletType.OTHER, Money.ZERO, Colors.paletteColor(wallets.size()));
            id = batch.run(new SaveCommand<>(storage.wallets(), wallet, "新增錢包", Topic.WALLETS)).result().id();
            wallets.put(name, id);
            created[0]++;
        }
        return id;
    }

    private long categoryId(String name, CategoryKind kind, Map<String, Long> categories, CommandManager.Batch batch,
                            int[] created) {
        String key = kind + ":" + name;
        Long id = categories.get(key);
        if (id == null) {
            Category category = Category.create(name, kind, Colors.paletteColor(categories.size()), "");
            id = batch.run(new SaveCommand<>(storage.categories(), category, "新增分類", Topic.CATEGORIES))
                    .result().id();
            categories.put(key, id);
            created[1]++;
        }
        return id;
    }

    private Map<String, Integer> mapHeader(Csv.Row header) {
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < header.values().size(); i++) {
            String name = header.values().get(i).strip();
            String key = HEADER_ALIASES.getOrDefault(name.toLowerCase(Locale.ROOT), name);
            if (HEADER.contains(key)) {
                columns.putIfAbsent(key, i);
            }
        }
        for (String required : List.of("日期", "金額", "錢包")) {
            if (!columns.containsKey(required)) {
                throw new CsvFormatException(header.line(), "缺少「" + required + "」欄位，標題列應包含："
                        + String.join("、", HEADER));
            }
        }
        return columns;
    }

    private ImportRow parseRow(Csv.Row row, Map<String, Integer> columns) {
        String typeText = value(row, columns, "類型");
        Money signed = Money.parse(value(row, columns, "金額"));
        TransactionType type = typeText.isEmpty()
                ? (signed.isNegative() ? TransactionType.EXPENSE : TransactionType.INCOME)
                : TransactionType.parse(typeText);
        Money amount = signed.abs();
        String wallet = Csv.unprotectFormula(value(row, columns, "錢包"));
        String toWallet = Csv.unprotectFormula(value(row, columns, "轉入錢包"));
        String category = Csv.unprotectFormula(value(row, columns, "分類"));
        if (wallet.isEmpty()) {
            throw new CsvFormatException(row.line(), "錢包不能空白。");
        }
        if (type == TransactionType.TRANSFER && toWallet.isEmpty()) {
            throw new CsvFormatException(row.line(), "轉帳需要「轉入錢包」。");
        }
        if (type != TransactionType.TRANSFER && category.isEmpty()) {
            category = "其他";
        }
        if (type == TransactionType.TRANSFER && toWallet.equals(wallet)) {
            throw new CsvFormatException(row.line(), "轉出與轉入的錢包不能相同。");
        }
        Checks.requirePositive(amount, "金額");
        Checks.requireText(wallet, "錢包名稱", Wallet.MAX_NAME_LENGTH);
        if (type == TransactionType.TRANSFER) {
            Checks.requireText(toWallet, "轉入錢包名稱", Wallet.MAX_NAME_LENGTH);
        } else {
            Checks.requireText(category, "分類名稱", Category.MAX_NAME_LENGTH);
        }
        String note = Checks.optionalText(Csv.unprotectFormula(value(row, columns, "備註")), "備註",
                Transaction.MAX_NOTE_LENGTH);
        ImportRow parsed = new ImportRow(row.line(), parseDate(value(row, columns, "日期"), row.line()),
                parseTime(value(row, columns, "時間"), row.line()), type, amount, category, wallet, toWallet, note,
                false);
        return parsed;
    }

    private static String value(Csv.Row row, Map<String, Integer> columns, String name) {
        Integer index = columns.get(name);
        return index == null ? "" : row.get(index).strip();
    }

    private static LocalDate parseDate(String text, int line) {
        try {
            return text.contains("/") ? LocalDate.parse(text, SLASH_DATE) : LocalDate.parse(text, DASH_DATE);
        } catch (DateTimeParseException e) {
            throw new CsvFormatException(line, "日期格式不正確：「" + text + "」，請使用 2026-10-31。");
        }
    }

    private static LocalTime parseTime(String text, int line) {
        if (text.isEmpty()) {
            return LocalTime.MIDNIGHT;
        }
        try {
            return LocalTime.parse(text.length() == 4 ? "0" + text : text);
        } catch (DateTimeParseException e) {
            throw new CsvFormatException(line, "時間格式不正確：「" + text + "」，請使用 13:05。");
        }
    }

    private List<ImportRow> markDuplicates(List<ImportRow> rows) {
        if (rows.isEmpty()) {
            return rows;
        }
        LocalDate min = rows.stream().map(ImportRow::date).min(LocalDate::compareTo).orElseThrow();
        LocalDate max = rows.stream().map(ImportRow::date).max(LocalDate::compareTo).orElseThrow();
        Map<Long, String> walletNames = new HashMap<>();
        storage.wallets().findAll().forEach(w -> walletNames.put(w.id(), w.name()));
        Map<Long, String> categoryNames = new HashMap<>();
        storage.categories().findAll().forEach(c -> categoryNames.put(c.id(), c.name()));
        Set<String> existing = new HashSet<>();
        for (Transaction t : storage.transactions().findBetween(new DateRange(min, max))) {
            existing.add(key(t.date(), t.time(), t.type(), t.amount(), walletNames.get(t.walletId()),
                    t.isTransfer() ? walletNames.get(t.toWalletId()) : categoryNames.get(t.categoryId()), t.note()));
        }
        List<ImportRow> result = new ArrayList<>();
        for (ImportRow row : rows) {
            String key = key(row.date(), row.time(), row.type(), row.amount(), row.wallet(),
                    row.type() == TransactionType.TRANSFER ? row.toWallet() : row.category(), row.note());
            result.add(existing.contains(key) ? row.markDuplicate() : row);
        }
        return result;
    }

    private static String key(LocalDate date, LocalTime time, TransactionType type, Money amount, String wallet,
                              String counterpart, String note) {
        return String.join("|", date.toString(), time.format(TIME), type.name(), amount.toPlainString(),
                String.valueOf(wallet), String.valueOf(counterpart), note == null ? "" : note);
    }

    private static String readAll(Reader in) throws IOException {
        StringBuilder text = new StringBuilder();
        char[] buffer = new char[8192];
        int read;
        while ((read = in.read(buffer)) >= 0) {
            text.append(buffer, 0, read);
        }
        return text.toString();
    }
}
