-- V2：預算、定期交易與儲蓄目標（MySQL 8）

CREATE TABLE budgets (
    id          BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NULL,
    limit_cents BIGINT NOT NULL,
    UNIQUE KEY ux_budgets_category (category_id),
    CONSTRAINT fk_budgets_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT ck_budgets_limit CHECK (limit_cents > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE recurring_rules (
    id             BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(30)  NOT NULL,
    type           VARCHAR(10)  NOT NULL,
    amount_cents   BIGINT       NOT NULL,
    category_id    BIGINT       NULL,
    wallet_id      BIGINT       NOT NULL,
    to_wallet_id   BIGINT       NULL,
    note           VARCHAR(200) NOT NULL DEFAULT '',
    frequency      VARCHAR(10)  NOT NULL,
    interval_count INT          NOT NULL DEFAULT 1,
    start_date     DATE         NOT NULL,
    end_date       DATE         NULL,
    posted_count   INT          NOT NULL DEFAULT 0,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_recurring_category FOREIGN KEY (category_id) REFERENCES categories (id),
    CONSTRAINT fk_recurring_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (id),
    CONSTRAINT fk_recurring_to_wallet FOREIGN KEY (to_wallet_id) REFERENCES wallets (id),
    CONSTRAINT ck_recurring_amount CHECK (amount_cents > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE savings_goals (
    id           BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(30) NOT NULL,
    target_cents BIGINT      NOT NULL,
    saved_cents  BIGINT      NOT NULL DEFAULT 0,
    deadline     DATE        NULL,
    color        INT         NOT NULL DEFAULT 0,
    CONSTRAINT ck_goals_target CHECK (target_cents > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- 記錄交易是由哪一條定期規則自動產生
ALTER TABLE transactions ADD COLUMN recurring_rule_id BIGINT NULL;
