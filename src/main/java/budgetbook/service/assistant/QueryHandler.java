package budgetbook.service.assistant;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Chain of Responsibility：每個 handler 判斷自己能不能回答，第一個能回答的負責回答。
 */
interface QueryHandler {
    boolean canAnswer(Question question);

    String answer(Question question);

    /** 以「判斷條件 + 回答方式」組成一個 handler。 */
    static QueryHandler of(Predicate<Question> condition, Function<Question, String> answer) {
        return new QueryHandler() {
            @Override
            public boolean canAnswer(Question question) {
                return condition.test(question);
            }

            @Override
            public String answer(Question question) {
                return answer.apply(question);
            }
        };
    }
}
