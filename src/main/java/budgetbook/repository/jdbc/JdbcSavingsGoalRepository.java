package budgetbook.repository.jdbc;

import budgetbook.model.SavingsGoal;
import budgetbook.repository.SavingsGoalRepository;

import java.sql.ResultSet;
import java.sql.SQLException;

final class JdbcSavingsGoalRepository extends JdbcRepository<SavingsGoal> implements SavingsGoalRepository {
    JdbcSavingsGoalRepository(JdbcSession session) {
        super(session, "savings_goals", "儲蓄目標", "name", "target_cents", "saved_cents", "deadline", "color");
    }

    @Override
    protected SavingsGoal map(ResultSet rs) throws SQLException {
        return new SavingsGoal(rs.getLong("id"), rs.getString("name"), JdbcSession.money(rs, "target_cents"),
                JdbcSession.money(rs, "saved_cents"), JdbcSession.date(rs, "deadline"), rs.getInt("color"));
    }

    @Override
    protected Object[] values(SavingsGoal g) {
        return new Object[]{g.name(), g.target(), g.saved(), g.deadline(), g.color()};
    }

    @Override
    protected SavingsGoal withId(SavingsGoal goal, long id) {
        return goal.withId(id);
    }
}
