package budgetbook.repository.jdbc;

import budgetbook.model.DateRange;
import budgetbook.model.Money;
import budgetbook.model.Transaction;
import budgetbook.model.TransactionQuery;
import budgetbook.model.TransactionType;
import budgetbook.repository.TransactionRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class JdbcTransactionRepository extends JdbcRepository<Transaction> implements TransactionRepository {
    private static final String ORDER = "tx_date DESC, tx_time DESC, id DESC";

    JdbcTransactionRepository(JdbcSession session) {
        super(session, "transactions", "交易", "type", "tx_date", "tx_time", "amount_cents", "category_id",
                "wallet_id", "to_wallet_id", "note", "recurring_rule_id");
    }

    @Override
    protected Transaction map(ResultSet rs) throws SQLException {
        Long toWallet = JdbcSession.nullableLong(rs, "to_wallet_id");
        return new Transaction(rs.getLong("id"), TransactionType.valueOf(rs.getString("type")),
                JdbcSession.date(rs, "tx_date"), JdbcSession.time(rs, "tx_time"),
                JdbcSession.money(rs, "amount_cents"), JdbcSession.nullableLong(rs, "category_id"),
                rs.getLong("wallet_id"), toWallet, rs.getString("note"),
                JdbcSession.nullableLong(rs, "recurring_rule_id"));
    }

    @Override
    protected Object[] values(Transaction t) {
        return new Object[]{t.type(), t.date(), t.time(), t.amount(), t.categoryId(), t.walletId(),
                t.toWalletId(), t.note(), t.recurringRuleId()};
    }

    @Override
    protected Transaction withId(Transaction transaction, long id) {
        return transaction.withId(id);
    }

    @Override
    protected String defaultOrder() {
        return ORDER;
    }

    @Override
    public List<Transaction> find(TransactionQuery query) {
        StringBuilder sql = new StringBuilder("SELECT ").append(columnList("t."))
                .append(" FROM transactions t")
                .append(" LEFT JOIN categories c ON c.id = t.category_id")
                .append(" LEFT JOIN wallets w ON w.id = t.wallet_id")
                .append(" LEFT JOIN wallets w2 ON w2.id = t.to_wallet_id")
                .append(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (query.range() != null) {
            sql.append(" AND t.tx_date >= ? AND t.tx_date <= ?");
            params.add(query.range().start());
            params.add(query.range().end());
        }
        if (query.type() != null) {
            sql.append(" AND t.type = ?");
            params.add(query.type());
        }
        if (query.categoryId() != null) {
            sql.append(" AND t.category_id = ?");
            params.add(query.categoryId());
        }
        if (query.walletId() != null) {
            sql.append(" AND (t.wallet_id = ? OR t.to_wallet_id = ?)");
            params.add(query.walletId());
            params.add(query.walletId());
        }
        if (query.hasText()) {
            // 用 ! 當跳脫字元：SQLite 與 MySQL 對反斜線的處理不同，! 兩邊都一致
            String pattern = "%" + query.text().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            sql.append(" AND (t.note LIKE ? ESCAPE '!' OR c.name LIKE ? ESCAPE '!'")
                    .append(" OR w.name LIKE ? ESCAPE '!' OR w2.name LIKE ? ESCAPE '!')");
            for (int i = 0; i < 4; i++) {
                params.add(pattern);
            }
        }
        sql.append(" ORDER BY t.tx_date DESC, t.tx_time DESC, t.id DESC");
        return session.query(sql.toString(), this::map, params.toArray());
    }

    @Override
    public List<Transaction> findBetween(DateRange range) {
        return session.query(select() + " WHERE tx_date >= ? AND tx_date <= ? ORDER BY " + ORDER, this::map,
                range.start(), range.end());
    }

    @Override
    public List<Transaction> findRecent(int limit) {
        return session.query(select() + " ORDER BY " + ORDER + " LIMIT ?", this::map, limit);
    }

    @Override
    public Map<Long, Money> netChangeByWallet(LocalDate upTo) {
        String dateFilter = upTo == null ? "" : " AND tx_date <= ?";
        String sql = "SELECT wallet_id AS wid,"
                + " SUM(CASE WHEN type = 'INCOME' THEN amount_cents ELSE -amount_cents END) AS delta"
                + " FROM transactions WHERE 1 = 1" + dateFilter + " GROUP BY wallet_id"
                + " UNION ALL"
                + " SELECT to_wallet_id AS wid, SUM(amount_cents) AS delta"
                + " FROM transactions WHERE type = 'TRANSFER'" + dateFilter + " GROUP BY to_wallet_id";
        Object[] params = upTo == null ? new Object[0] : new Object[]{upTo, upTo};
        Map<Long, Money> result = new LinkedHashMap<>();
        session.query(sql, rs -> result.merge(rs.getLong("wid"), Money.ofCents(rs.getLong("delta")), Money::plus),
                params);
        return result;
    }

    @Override
    public long countByWallet(long walletId) {
        return session.queryLong("SELECT COUNT(*) FROM transactions WHERE wallet_id = ? OR to_wallet_id = ?",
                walletId, walletId);
    }

    @Override
    public long countByCategory(long categoryId) {
        return session.queryLong("SELECT COUNT(*) FROM transactions WHERE category_id = ?", categoryId);
    }

    @Override
    public Optional<LocalDate> earliestDate() {
        return session.queryOne("SELECT MIN(tx_date) AS first_date FROM transactions",
                rs -> JdbcSession.date(rs, "first_date"));
    }

    @Override
    public List<String> recentNotes(int limit) {
        return session.query("SELECT note, MAX(id) AS last_id FROM transactions WHERE note <> ''"
                + " GROUP BY note ORDER BY last_id DESC LIMIT ?", rs -> rs.getString("note"), limit);
    }
}
