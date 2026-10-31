package budgetbook.repository.jdbc;

import budgetbook.model.Budget;
import budgetbook.repository.BudgetRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

final class JdbcBudgetRepository extends JdbcRepository<Budget> implements BudgetRepository {
    JdbcBudgetRepository(JdbcSession session) {
        super(session, "budgets", "預算", "category_id", "limit_cents");
    }

    @Override
    protected Budget map(ResultSet rs) throws SQLException {
        return new Budget(rs.getLong("id"), JdbcSession.nullableLong(rs, "category_id"),
                JdbcSession.money(rs, "limit_cents"));
    }

    @Override
    protected Object[] values(Budget b) {
        return new Object[]{b.categoryId(), b.monthlyLimit()};
    }

    @Override
    protected Budget withId(Budget budget, long id) {
        return budget.withId(id);
    }

    @Override
    public Optional<Budget> findByCategoryId(Long categoryId) {
        if (categoryId == null) {
            return session.queryOne(select() + " WHERE category_id IS NULL", this::map);
        }
        return session.queryOne(select() + " WHERE category_id = ?", this::map, categoryId);
    }
}
