package budgetbook.service;

import budgetbook.command.CommandManager;
import budgetbook.command.DeleteCommand;
import budgetbook.command.SaveCommand;
import budgetbook.event.Topic;
import budgetbook.exception.EntityInUseException;
import budgetbook.exception.ValidationException;
import budgetbook.model.Money;
import budgetbook.model.Wallet;
import budgetbook.model.WalletType;
import budgetbook.repository.Storage;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 錢包管理與餘額計算。 */
public final class WalletService {
    private final Storage storage;
    private final CommandManager commands;

    public WalletService(Storage storage, CommandManager commands) {
        this.storage = storage;
        this.commands = commands;
    }

    public List<Wallet> all() {
        return storage.wallets().findAll();
    }

    /** 未封存的錢包，記帳時只能選這些。 */
    public List<Wallet> active() {
        return all().stream().filter(wallet -> !wallet.archived()).toList();
    }

    public Wallet get(long id) {
        return storage.wallets().getById(id);
    }

    public Wallet create(String name, WalletType type, Money initialBalance, int color) {
        Wallet wallet = Wallet.create(name, type, initialBalance, color);
        ensureUniqueName(wallet);
        return commands.execute(new SaveCommand<>(storage.wallets(), wallet,
                "新增錢包「" + wallet.name() + "」", Topic.WALLETS)).result();
    }

    public Wallet update(Wallet wallet) {
        ensureUniqueName(wallet);
        return commands.execute(new SaveCommand<>(storage.wallets(), wallet,
                "修改錢包「" + wallet.name() + "」", Topic.WALLETS, Topic.TRANSACTIONS)).result();
    }

    public Wallet setArchived(long id, boolean archived) {
        Wallet wallet = get(id).withArchived(archived);
        return commands.execute(new SaveCommand<>(storage.wallets(), wallet,
                (archived ? "封存錢包「" : "取消封存「") + wallet.name() + "」", Topic.WALLETS)).result();
    }

    /** 只有沒有任何交易與定期交易的錢包可以刪除，否則請改用封存。 */
    public void delete(long id) {
        Wallet wallet = get(id);
        long used = storage.transactions().countByWallet(id) + storage.recurringRules().countByWallet(id);
        if (used > 0) {
            throw new EntityInUseException("「" + wallet.name() + "」還有 " + used
                    + " 筆交易或定期交易，不能刪除；可以改用「封存」把它從選單隱藏。");
        }
        commands.execute(new DeleteCommand<>(storage.wallets(), id, "刪除錢包「" + wallet.name() + "」",
                Topic.WALLETS));
    }

    /** 所有錢包目前的餘額（初始金額 + 交易增減）。 */
    public Map<Long, Money> balances() {
        return balancesAsOf(null);
    }

    /** 指定日期（含）結束時的餘額；null 代表包含所有交易。 */
    public Map<Long, Money> balancesAsOf(LocalDate date) {
        Map<Long, Money> changes = storage.transactions().netChangeByWallet(date);
        Map<Long, Money> balances = new LinkedHashMap<>();
        for (Wallet wallet : all()) {
            balances.put(wallet.id(), wallet.initialBalance().plus(changes.getOrDefault(wallet.id(), Money.ZERO)));
        }
        return balances;
    }

    public Money balanceOf(long walletId) {
        return balances().getOrDefault(walletId, Money.ZERO);
    }

    public Money totalBalance() {
        return Money.sum(balances().values().stream());
    }

    public Money totalBalanceAsOf(LocalDate date) {
        return Money.sum(balancesAsOf(date).values().stream());
    }

    private void ensureUniqueName(Wallet wallet) {
        storage.wallets().findByName(wallet.name())
                .filter(other -> other.id() != wallet.id())
                .ifPresent(other -> {
                    throw new ValidationException("錢包名稱", "已經有名為「" + other.name() + "」的錢包。");
                });
    }
}
