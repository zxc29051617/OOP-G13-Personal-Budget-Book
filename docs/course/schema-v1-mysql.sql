CREATE DATABASE IF NOT EXISTS personal_budget_book
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE personal_budget_book;

CREATE TABLE IF NOT EXISTS accounts (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(100) NOT NULL,
    initial_balance DOUBLE NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS transactions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    date DATE NOT NULL,
    transaction_time TIME NOT NULL DEFAULT '00:00:00',
    kind VARCHAR(20) NOT NULL,
    category VARCHAR(100) NOT NULL,
    account_id INT NOT NULL,
    amount DOUBLE NOT NULL,
    note VARCHAR(255),
    CONSTRAINT fk_transactions_account
        FOREIGN KEY (account_id) REFERENCES accounts(id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS settings (
    id INT PRIMARY KEY,
    monthly_limit DOUBLE NOT NULL,
    points INT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO settings (id, monthly_limit, points)
VALUES (1, 10000, 0)
ON DUPLICATE KEY UPDATE id = id;
