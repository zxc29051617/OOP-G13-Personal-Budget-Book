package budgetbook.repository.jdbc;

import budgetbook.model.Wallet;
import budgetbook.model.WalletType;
import budgetbook.repository.WalletRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

final class JdbcWalletRepository extends JdbcRepository<Wallet> implements WalletRepository {
    JdbcWalletRepository(JdbcSession session) {
        super(session, "wallets", "錢包", "name", "type", "initial_cents", "color", "archived");
    }

    @Override
    protected Wallet map(ResultSet rs) throws SQLException {
        return new Wallet(rs.getLong("id"), rs.getString("name"), WalletType.valueOf(rs.getString("type")),
                JdbcSession.money(rs, "initial_cents"), rs.getInt("color"), rs.getBoolean("archived"));
    }

    @Override
    protected Object[] values(Wallet w) {
        return new Object[]{w.name(), w.type(), w.initialBalance(), w.color(), w.archived()};
    }

    @Override
    protected Wallet withId(Wallet wallet, long id) {
        return wallet.withId(id);
    }

    @Override
    protected String defaultOrder() {
        return "archived, id";
    }

    @Override
    public Optional<Wallet> findByName(String name) {
        return session.queryOne(select() + " WHERE name = ?", this::map, name.strip());
    }
}
