package budgetbook.service;

import budgetbook.Fixtures;
import budgetbook.model.Money;
import budgetbook.model.SavingsGoal;
import budgetbook.service.assistant.QueryAssistantService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static budgetbook.Fixtures.TODAY;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryAssistantServiceTest {
    private final AppContext app = Fixtures.app();

    @BeforeEach
    void setUp() {
        Fixtures.income(app, "薪資", 40000, TODAY.withDayOfMonth(5));
        Fixtures.expense(app, "餐飲", 300, TODAY.withDayOfMonth(1), "早午餐");
        Fixtures.expense(app, "餐飲", 700, TODAY.withDayOfMonth(2), "聚餐");
        Fixtures.expense(app, "交通", 1500, TODAY.withDayOfMonth(3), "高鐵");
        Fixtures.expense(app, "餐飲", 400, LocalDate.of(2026, 9, 10), "九月");
        Fixtures.expense(app, "購物", 2000, LocalDate.of(2026, 3, 8), "三月");
        app.budgets().setOverall(Money.of(3000));
        app.goals().save(new SavingsGoal(0, "日本旅行", Money.of(60000), Money.of(15000), LocalDate.of(2027, 3, 31), 0));
    }

    @AfterEach
    void tearDown() {
        app.close();
    }

    private void assertAnswer(String question, String... expected) {
        String answer = app.assistant().answer(question);
        for (String text : expected) {
            assertTrue(answer.contains(text), () -> "「" + question + "」的回答應包含「" + text + "」，實際：" + answer);
        }
    }

    @Test
    void answersFromTheUsersData() {
        assertAnswer("本月花最多的是哪個分類？", "交通", "$1,500", "60%");
        assertAnswer("這個月餐飲花了多少？", "「餐飲」支出 $1,000", "共 2 筆", "40%");
        assertAnswer("上個月餐飲花多少", "2026 年 9 月「餐飲」支出 $400");
        assertAnswer("三月花了多少錢", "2026 年 3 月支出 $2,000");
        assertAnswer("2026年9月的支出", "2026 年 9 月支出 $400");
        assertAnswer("預算還剩多少？", "總預算 $3,000", "已用 $2,500", "還剩 $500", "月底預估支出 $5,167");
        assertAnswer("跟上個月比支出增加了嗎", "多了 $2,100", "+525%");
        assertAnswer("目前總資產是多少？", "$54,100", "$15,000 已標記給儲蓄目標");
        assertAnswer("日本旅行還差多少", "還差 $45,000", "每月約需存 $7,500");
        assertAnswer("本月收入", "收入 $40,000", "薪資");
        assertAnswer("本月收入和支出", "結餘 $37,500");
        assertAnswer("最近 7 天平均每天花多少", "最近 7 天平均每天支出 $0");
        assertAnswer("本月最貴的一筆", "高鐵", "$1,500");
        assertAnswer("現金還有多少", "「現金」目前餘額 -$1,900");
        assertAnswer("我有幾個錢包", "3 個錢包", "合計 $54,100");
        assertAnswer("小樹長得怎樣", "本月已澆水 4 / 20 次", "再記 16 筆");
        assertAnswer("最近一筆交易", "高鐵");
        assertAnswer("今年記了幾筆", "今年共記了 6 筆");
        assertAnswer("接下來有哪些定期扣款", "沒有定期交易");
    }

    @Test
    void helpAndFallback() {
        assertAnswer("", "可以這樣問");
        assertAnswer("今天天氣好嗎", "看不懂");
        assertTrue(QueryAssistantService.helpText().contains("上個月"));
    }
}
