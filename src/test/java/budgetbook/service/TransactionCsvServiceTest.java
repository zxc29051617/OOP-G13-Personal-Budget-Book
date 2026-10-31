package budgetbook.service;

import budgetbook.Fixtures;
import budgetbook.exception.CsvFormatException;
import budgetbook.model.CategoryKind;
import budgetbook.model.Money;
import budgetbook.model.Transaction;
import budgetbook.model.TransactionQuery;
import budgetbook.model.TransactionType;
import budgetbook.service.io.Csv;
import budgetbook.service.io.ImportPreview;
import budgetbook.service.io.ImportResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import static budgetbook.Fixtures.TODAY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionCsvServiceTest {
    private final AppContext app = Fixtures.app();

    @AfterEach
    void tearDown() {
        app.close();
    }

    @Test
    void exportThenImportIntoEmptyDatabase() throws IOException {
        Fixtures.expense(app, "餐飲", 120, TODAY, "午餐, 加蛋 \"大份\"");
        Fixtures.income(app, "薪資", 40000, TODAY);
        app.transactions().add(Transaction.builder().transfer(Fixtures.wallet(app, "銀行帳戶").id(),
                Fixtures.wallet(app, "現金").id()).date(TODAY).amount(3000).note("=提款").build());
        List<Transaction> all = app.transactions().search(TransactionQuery.all());
        String csv = app.csv().exportToString(all);
        assertTrue(csv.startsWith("﻿日期,時間,類型,金額,分類,錢包,轉入錢包,備註"));
        assertTrue(csv.contains("'=提款"), "formula injection is neutralised");

        try (AppContext other = Fixtures.emptyApp()) {
            ImportResult result = other.csv().importCsv(new StringReader(csv));
            assertEquals(3, result.imported());
            assertEquals(2, result.createdWallets());
            assertEquals(2, result.createdCategories());
            List<Transaction> imported = other.transactions().search(TransactionQuery.all());
            assertTrue(imported.stream().anyMatch(t -> t.note().equals("午餐, 加蛋 \"大份\"")));
            assertTrue(imported.stream().anyMatch(t -> t.note().equals("=提款") && t.isTransfer()));
            assertEquals(Money.of(40000 - 120), other.wallets().totalBalance());

            ImportResult again = other.csv().importCsv(new StringReader(csv));
            assertEquals(0, again.imported());
            assertEquals(3, again.skippedDuplicates());
            assertTrue(again.summary().contains("略過重複 3 筆"));

            other.commands().undo();
            other.commands().undo();
            assertEquals(0, other.transactions().count());
            assertEquals(0, other.wallets().all().size());
        }
    }

    @Test
    void acceptsEnglishHeadersAnyOrderAndSignedAmounts() throws IOException {
        String csv = """
                note,amount,date,account,category
                咖啡,-65,2026/10/3,現金,餐飲
                退款,200,2026-10-04,現金,其他
                """;
        ImportPreview preview = app.csv().preview(new StringReader(csv));
        assertEquals(2, preview.newCount());
        assertEquals(TransactionType.EXPENSE, preview.rows().get(0).type());
        assertEquals(Money.of(65), preview.rows().get(0).amount());
        assertEquals(TransactionType.INCOME, preview.rows().get(1).type());
        ImportResult result = app.csv().importRows(preview);
        assertEquals(2, result.imported());
        assertEquals(0, result.createdCategories(), "existing 支出/收入「其他」 are reused");
        assertEquals(Fixtures.category(app, CategoryKind.EXPENSE, "餐飲").id(),
                app.transactions().recent(2).get(1).categoryId());
    }

    @Test
    void reportsBadRowsWithLineNumbers() throws IOException {
        String csv = """
                日期,類型,金額,錢包,分類
                2026-10-01,支出,abc,現金,餐飲
                2026-13-01,支出,10,現金,餐飲
                2026-10-02,轉帳,10,現金,
                2026-10-03,支出,0,現金,餐飲
                2026-10-04,支出,10,現金,餐飲
                """;
        ImportPreview preview = app.csv().preview(new StringReader(csv));
        assertEquals(1, preview.rows().size());
        assertEquals(4, preview.errors().size());
        assertTrue(preview.errors().get(0).startsWith("第 2 行"));
        assertTrue(preview.errors().get(2).contains("轉入錢包"));
    }

    @Test
    void missingRequiredColumnsAreRejected() {
        assertThrows(CsvFormatException.class, () -> app.csv().preview(new StringReader("日期,備註\n2026-10-01,x\n")));
        assertThrows(CsvFormatException.class, () -> app.csv().preview(new StringReader("")));
    }

    @Test
    void csvParserHandlesQuotesNewlinesAndBom() {
        List<Csv.Row> rows = Csv.parse("﻿a,b\r\n\"x,1\",\"line1\nline2\"\n\n\"say \"\"hi\"\"\",\n");
        assertEquals(3, rows.size());
        assertEquals(List.of("x,1", "line1\nline2"), rows.get(1).values());
        assertEquals(2, rows.get(1).line());
        assertEquals(List.of("say \"hi\"", ""), rows.get(2).values());
        assertEquals(5, rows.get(2).line());
        assertThrows(CsvFormatException.class, () -> Csv.parse("\"open"));
        assertEquals("\"a\"\"b\"", Csv.escape("a\"b"));
        assertEquals("'+886", Csv.protectFormula("+886"));
        assertEquals("+886", Csv.unprotectFormula("'+886"));
    }
}
