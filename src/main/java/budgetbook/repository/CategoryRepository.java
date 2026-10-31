package budgetbook.repository;

import budgetbook.model.Category;
import budgetbook.model.CategoryKind;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends Repository<Category> {
    List<Category> findByKind(CategoryKind kind);

    Optional<Category> findByName(CategoryKind kind, String name);
}
