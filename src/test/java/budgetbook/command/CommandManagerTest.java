package budgetbook.command;

import budgetbook.event.DataChangedEvent;
import budgetbook.event.EventBus;
import budgetbook.event.HistoryChangedEvent;
import budgetbook.event.Topic;
import budgetbook.exception.ValidationException;
import budgetbook.model.Money;
import budgetbook.model.Wallet;
import budgetbook.model.WalletType;
import budgetbook.repository.Storage;
import budgetbook.repository.StorageConfig;
import budgetbook.repository.StorageFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandManagerTest {
    private Storage storage;
    private EventBus bus;
    private CommandManager commands;
    private final List<Object> events = new ArrayList<>();

    @BeforeEach
    void setUp() {
        storage = StorageFactory.open(StorageConfig.inMemory());
        bus = new EventBus();
        bus.subscribe(Object.class, events::add);
        commands = new CommandManager(storage, bus, 3);
    }

    @AfterEach
    void tearDown() {
        storage.close();
    }

    private SaveCommand<Wallet> addWallet(String name) {
        return new SaveCommand<>(storage.wallets(), Wallet.create(name, WalletType.CASH, Money.of(100), 0),
                "新增 " + name, Topic.WALLETS);
    }

    @Test
    void undoAndRedoInsertKeepTheSameId() {
        Wallet wallet = commands.execute(addWallet("現金")).result();
        commands.undo();
        assertEquals(0, storage.wallets().count());
        commands.redo();
        assertEquals(wallet, storage.wallets().getById(wallet.id()));
    }

    @Test
    void undoUpdateRestoresPreviousVersion() {
        Wallet wallet = commands.execute(addWallet("現金")).result();
        commands.execute(new SaveCommand<>(storage.wallets(), wallet.withName("零錢包"), "改名", Topic.WALLETS));
        commands.undo();
        assertEquals("現金", storage.wallets().getById(wallet.id()).name());
        commands.redo();
        assertEquals("零錢包", storage.wallets().getById(wallet.id()).name());
    }

    @Test
    void undoDeleteRestoresRow() {
        Wallet wallet = commands.execute(addWallet("現金")).result();
        commands.execute(new DeleteCommand<>(storage.wallets(), wallet.id(), "刪除", Topic.WALLETS));
        assertEquals(0, storage.wallets().count());
        commands.undo();
        assertEquals(wallet, storage.wallets().getById(wallet.id()));
    }

    @Test
    void newCommandClearsRedoAndHistoryIsLimited() {
        commands.execute(addWallet("A"));
        commands.undo();
        assertTrue(commands.canRedo());
        commands.execute(addWallet("B"));
        assertFalse(commands.canRedo());
        commands.execute(addWallet("C"));
        commands.execute(addWallet("D"));
        commands.execute(addWallet("E"));
        assertEquals(3, commands.undoSize());
        assertEquals("新增 E", commands.undoDescription().orElseThrow());
    }

    @Test
    void batchIsOneUndoStepAndAtomic() {
        commands.executeBatch("兩個錢包", batch -> {
            batch.run(addWallet("A"));
            return batch.run(addWallet("B")).result();
        });
        assertEquals(2, storage.wallets().count());
        commands.undo();
        assertEquals(0, storage.wallets().count());
        commands.redo();
        assertEquals(2, storage.wallets().count());

        assertThrows(ValidationException.class, () -> commands.executeBatch("失敗", batch -> {
            batch.run(addWallet("C"));
            throw new ValidationException("x", "第二步失敗");
        }));
        assertEquals(2, storage.wallets().count());
        assertEquals("兩個錢包", commands.undoDescription().orElseThrow());
    }

    @Test
    void failedCommandLeavesNoTrace() {
        commands.execute(addWallet("現金"));
        assertThrows(RuntimeException.class, () -> commands.execute(addWallet("現金")));
        assertEquals(1, storage.wallets().count());
        assertEquals(1, commands.undoSize());
    }

    @Test
    void publishesEvents() {
        commands.execute(addWallet("現金"));
        DataChangedEvent changed = events.stream().filter(DataChangedEvent.class::isInstance)
                .map(DataChangedEvent.class::cast).findFirst().orElseThrow();
        assertTrue(changed.affects(Topic.WALLETS));
        HistoryChangedEvent history = events.stream().filter(HistoryChangedEvent.class::isInstance)
                .map(HistoryChangedEvent.class::cast).reduce((a, b) -> b).orElseThrow();
        assertTrue(history.canUndo());
        assertEquals("新增 現金", history.undoLabel());
    }

    @Test
    void eventBusIsolatesFailingSubscribers() {
        List<String> received = new ArrayList<>();
        bus.subscribe(String.class, s -> {
            throw new IllegalStateException("bad subscriber");
        });
        EventBus.Subscription subscription = bus.subscribe(String.class, received::add);
        bus.publish("hello");
        subscription.close();
        bus.publish("again");
        assertEquals(List.of("hello"), received);
    }
}
