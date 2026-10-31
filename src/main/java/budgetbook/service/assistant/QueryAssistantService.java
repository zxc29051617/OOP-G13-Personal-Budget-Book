package budgetbook.service.assistant;

import java.util.List;

/**
 * 記帳查詢助手：以規則判斷問題的意圖，再用 service 從自己的資料算出答案。
 * 不呼叫任何外部 AI 服務，回答一定來自資料庫中的資料。
 */
public final class QueryAssistantService {
    public static final List<String> SAMPLE_QUESTIONS = List.of(
            "本月花最多的是哪個分類？",
            "這個月餐飲花了多少？",
            "預算還剩多少？",
            "跟上個月比，支出增加了嗎？",
            "目前總資產是多少？",
            "最近 7 天平均每天花多少？",
            "儲蓄目標進度如何？",
            "接下來有哪些定期扣款？",
            "本月小樹長得怎樣？",
            "最近一筆交易是什麼？");

    private final AssistantServices services;
    private final List<QueryHandler> handlers;

    public QueryAssistantService(AssistantServices services) {
        this.services = services;
        Answers a = new Answers(services);
        this.handlers = List.of(
                QueryHandler.of(q -> q.isBlank() || q.has("可以問", "能問", "怎麼用", "幫助", "help", "說明", "功能"),
                        q -> helpText()),
                QueryHandler.of(q -> q.goal() != null, a::goals),
                QueryHandler.of(q -> q.has("比較", "相比", "比上", "增加", "減少", "多花", "少花", "變化"), a::compare),
                QueryHandler.of(q -> q.has("預算", "超支", "還能花", "可以花"), a::budget),
                QueryHandler.of(q -> q.has("花最多", "最多錢", "最大宗", "哪個分類", "哪一類", "排行", "前三", "占比", "比例"),
                        a::topCategories),
                QueryHandler.of(q -> q.has("最大一筆", "最大筆", "最貴", "最高的一筆", "單筆最"), a::largestExpense),
                QueryHandler.of(q -> q.has("目標", "存錢", "儲蓄", "存到"), a::goals),
                QueryHandler.of(q -> q.has("定期", "固定", "訂閱", "月租", "扣款", "帳單", "即將", "下次", "接下來"),
                        a::recurring),
                QueryHandler.of(q -> q.has("樹", "澆水", "森林", "連續"), a::tree),
                QueryHandler.of(q -> q.category() != null, a::category),
                QueryHandler.of(q -> q.wallet() != null, a::wallet),
                QueryHandler.of(q -> q.has("錢包", "帳戶"), q -> a.walletList()),
                QueryHandler.of(q -> q.has("餘額", "資產", "有多少錢", "還有多少", "剩多少", "淨值", "存款"),
                        q -> a.balance()),
                QueryHandler.of(q -> q.has("平均", "每天花", "日均", "一天花"), a::average),
                QueryHandler.of(q -> (q.has("收入") && q.has("支出")) || q.has("結餘", "收支", "存了多少", "淨收入"),
                        a::net),
                QueryHandler.of(q -> q.has("收入", "賺", "薪水", "進帳"), a::income),
                QueryHandler.of(q -> q.has("支出", "花", "開銷", "花費", "消費", "用了多少", "用掉"), a::expense),
                QueryHandler.of(q -> q.has("幾筆", "筆數", "多少筆", "記了幾"), a::count),
                QueryHandler.of(q -> q.has("最近", "最新", "上一筆", "最後一筆", "剛剛"), q -> a.recent()));
    }

    public String answer(String question) {
        Question parsed = Question.parse(question, services);
        for (QueryHandler handler : handlers) {
            if (handler.canAnswer(parsed)) {
                return handler.answer(parsed);
            }
        }
        return "我還看不懂這個問題，換個說法試試看？\n\n" + helpText();
    }

    public static String helpText() {
        StringBuilder text = new StringBuilder("我會用你的記帳資料回答問題，可以這樣問：");
        for (String sample : SAMPLE_QUESTIONS) {
            text.append("\n・").append(sample);
        }
        return text.append("\n\n時間可以說：今天、本週、上個月、3 月、2026 年 5 月、最近 30 天、今年。").toString();
    }
}
