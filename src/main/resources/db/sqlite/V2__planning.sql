-- V2：預算、定期交易與儲蓄目標（SQLite）

CREATE TABLE budgets (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    category_id INTEGER REFERENCES categories (id) ON DELETE RESTRICT,
    limit_cents INTEGER NOT NULL CHECK (limit_cents > 0)
);

CREATE UNIQUE INDEX ux_budgets_category ON budgets (category_id);

CREATE TABLE recurring_rules (
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    name           TEXT    NOT NULL,
    type           TEXT    NOT NULL CHECK (type IN ('EXPENSE', 'INCOME', 'TRANSFER')),
    amount_cents   INTEGER NOT NULL CHECK (amount_cents > 0),
    category_id    INTEGER REFERENCES categories (id) ON DELETE RESTRICT,
    wallet_id      INTEGER NOT NULL REFERENCES wallets (id) ON DELETE RESTRICT,
    to_wallet_id   INTEGER REFERENCES wallets (id) ON DELETE RESTRICT,
    note           TEXT    NOT NULL DEFAULT '',
    frequency      TEXT    NOT NULL,
    interval_count INTEGER NOT NULL DEFAULT 1,
    start_date     TEXT    NOT NULL,
    end_date       TEXT,
    posted_count   INTEGER NOT NULL DEFAULT 0,
    active         INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE savings_goals (
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    name         TEXT    NOT NULL,
    target_cents INTEGER NOT NULL CHECK (target_cents > 0),
    saved_cents  INTEGER NOT NULL DEFAULT 0,
    deadline     TEXT,
    color        INTEGER NOT NULL DEFAULT 0
);

-- 記錄交易是由哪一條定期規則自動產生
ALTER TABLE transactions ADD COLUMN recurring_rule_id INTEGER;
