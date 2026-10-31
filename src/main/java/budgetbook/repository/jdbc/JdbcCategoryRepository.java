package budgetbook.repository.jdbc;

import budgetbook.model.Category;
import budgetbook.model.CategoryKind;
import budgetbook.repository.CategoryRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

final class JdbcCategoryRepository extends JdbcRepository<Category> implements CategoryRepository {
    JdbcCategoryRepository(JdbcSession session) {
        super(session, "categories", "分類", "name", "kind", "color", "icon");
    }

    @Override
    protected Category map(ResultSet rs) throws SQLException {
        return new Category(rs.getLong("id"), rs.getString("name"), CategoryKind.valueOf(rs.getString("kind")),
                rs.getInt("color"), rs.getString("icon"));
    }

    @Override
    protected Object[] values(Category c) {
        return new Object[]{c.name(), c.kind(), c.color(), c.icon()};
    }

    @Override
    protected Category withId(Category category, long id) {
        return category.withId(id);
    }

    @Override
    protected String defaultOrder() {
        return "kind, id";
    }

    @Override
    public List<Category> findByKind(CategoryKind kind) {
        return session.query(select() + " WHERE kind = ? ORDER BY id", this::map, kind);
    }

    @Override
    public Optional<Category> findByName(CategoryKind kind, String name) {
        return session.queryOne(select() + " WHERE kind = ? AND name = ?", this::map, kind, name.strip());
    }
}
