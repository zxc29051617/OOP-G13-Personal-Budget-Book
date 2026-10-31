package budgetbook.repository;

import budgetbook.model.Wallet;

import java.util.Optional;

public interface WalletRepository extends Repository<Wallet> {
    Optional<Wallet> findByName(String name);
}
