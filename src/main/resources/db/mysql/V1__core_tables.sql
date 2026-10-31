-- V1：錢包、分類、交易與設定（MySQL 8）
-- 金額以「分」為單位的 BIGINT 保存，避免浮點數誤差。

CREATE TABLE wallets (
    id            BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(30) NOT NULL,
    type          VARCHAR(20) NOT NULL,
    initial_cents BIGINT      NOT NULL DEFAULT 0,
    color         INT         NOT NULL DEFAULT 0,
    archived      BOOLEAN     NOT NULL DEFAULT FALSE,
    UNIQUE KEY ux_wallets_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE categories (
    id    BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name  VARCHAR(20) NOT NULL,
    kind  VARCHAR(10) NOT NULL,
    color INT         NOT NULL DEFAULT 0,
    icon  VARCHAR(16) NOT NULL DEFAULT '',
    UNIQUE KEY ux_categories_kind_name (kind, name),
    CONSTRAINT ck_categories_kind CHECK (kind IN ('EXPENSE', 'INCOME'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE transactions (
    id           BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    type         VARCHAR(10)  NOT NULL,
    tx_date      DATE         NOT NULL,
    tx_time      TIME         NOT NULL DEFAULT '00:00:00',
    amount_cents BIGINT       NOT NULL,
    category_id  BIGINT       NULL,
    wallet_id    BIGINT       NOT NULL,
    to_wallet_id BIGINT       NULL,
    note         VARCHAR(200) NOT NULL DEFAULT '',
    INDEX idx_transactions_date (tx_date),
    CONSTRAINT fk_transactions_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_transactions_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (id),
    CONSTRAINT fk_transactions_to_wallet FOREIGN KEY (to_wallet_id) REFERENCES wallets (id),
    CONSTRAINT ck_transactions_type CHECK (type IN ('EXPENSE', 'INCOME', 'TRANSFER')),
    CONSTRAINT ck_transactions_amount CHECK (amount_cents > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE settings (
    setting_key   VARCHAR(64)  NOT NULL PRIMARY KEY,
    setting_value VARCHAR(500) NOT NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
