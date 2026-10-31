-- V1：錢包、分類、交易與設定（SQLite）
-- 金額以「分」為單位的整數保存；日期 YYYY-MM-DD、時間 HH:mm 以文字保存，可直接排序。

CREATE TABLE wallets (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    name          TEXT    NOT NULL UNIQUE,
    type          TEXT    NOT NULL,
    initial_cents INTEGER NOT NULL DEFAULT 0,
    color         INTEGER NOT NULL DEFAULT 0,
    archived      INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE categories (
    id    INTEGER PRIMARY KEY AUTOINCREMENT,
    name  TEXT    NOT NULL,
    kind  TEXT    NOT NULL CHECK (kind IN ('EXPENSE', 'INCOME')),
    color INTEGER NOT NULL DEFAULT 0,
    icon  TEXT    NOT NULL DEFAULT '',
    UNIQUE (kind, name)
);

CREATE TABLE transactions (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    type         TEXT    NOT NULL CHECK (type IN ('EXPENSE', 'INCOME', 'TRANSFER')),
    tx_date      TEXT    NOT NULL,
    tx_time      TEXT    NOT NULL DEFAULT '00:00',
    amount_cents INTEGER NOT NULL CHECK (amount_cents > 0),
    category_id  INTEGER REFERENCES categories (id) ON DELETE RESTRICT,
    wallet_id    INTEGER NOT NULL REFERENCES wallets (id) ON DELETE RESTRICT,
    to_wallet_id INTEGER REFERENCES wallets (id) ON DELETE RESTRICT,
    note         TEXT    NOT NULL DEFAULT ''
);

CREATE INDEX idx_transactions_date ON transactions (tx_date);
CREATE INDEX idx_transactions_wallet ON transactions (wallet_id);
CREATE INDEX idx_transactions_category ON transactions (category_id);

CREATE TABLE settings (
    setting_key   TEXT PRIMARY KEY,
    setting_value TEXT NOT NULL
);
