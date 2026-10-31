package budgetbook.repository.jdbc;

import budgetbook.model.Frequency;
import budgetbook.model.RecurringRule;
import budgetbook.model.TransactionType;
import budgetbook.repository.RecurringRuleRepository;

import java.sql.ResultSet;
import java.sql.SQLException;

final class JdbcRecurringRuleRepository extends JdbcRepository<RecurringRule> implements RecurringRuleRepository {
    JdbcRecurringRuleRepository(JdbcSession session) {
        super(session, "recurring_rules", "定期交易", "name", "type", "amount_cents", "category_id", "wallet_id",
                "to_wallet_id", "note", "frequency", "interval_count", "start_date", "end_date", "posted_count",
                "active");
    }

    @Override
    protected RecurringRule map(ResultSet rs) throws SQLException {
        return new RecurringRule(rs.getLong("id"), rs.getString("name"),
                TransactionType.valueOf(rs.getString("type")), JdbcSession.money(rs, "amount_cents"),
                JdbcSession.nullableLong(rs, "category_id"), rs.getLong("wallet_id"),
                JdbcSession.nullableLong(rs, "to_wallet_id"), rs.getString("note"),
                Frequency.valueOf(rs.getString("frequency")), rs.getInt("interval_count"),
                JdbcSession.date(rs, "start_date"), JdbcSession.date(rs, "end_date"), rs.getInt("posted_count"),
                rs.getBoolean("active"));
    }

    @Override
    protected Object[] values(RecurringRule r) {
        return new Object[]{r.name(), r.type(), r.amount(), r.categoryId(), r.walletId(), r.toWalletId(), r.note(),
                r.frequency(), r.interval(), r.startDate(), r.endDate(), r.postedCount(), r.active()};
    }

    @Override
    protected RecurringRule withId(RecurringRule rule, long id) {
        return rule.withId(id);
    }

    @Override
    public long countByWallet(long walletId) {
        return session.queryLong("SELECT COUNT(*) FROM recurring_rules WHERE wallet_id = ? OR to_wallet_id = ?",
                walletId, walletId);
    }

    @Override
    public long countByCategory(long categoryId) {
        return session.queryLong("SELECT COUNT(*) FROM recurring_rules WHERE category_id = ?", categoryId);
    }
}
