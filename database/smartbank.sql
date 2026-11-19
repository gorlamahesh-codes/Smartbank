CREATE DATABASE IF NOT EXISTS smartbank_db;
USE smartbank_db;

CREATE TABLE users (
 user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
 username VARCHAR(50) NOT NULL UNIQUE,
 password_hash VARCHAR(100) NOT NULL,
 role ENUM('ADMIN','CUSTOMER') NOT NULL DEFAULT 'CUSTOMER',
 active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE customers (
 customer_id BIGINT PRIMARY KEY AUTO_INCREMENT,
 user_id BIGINT NOT NULL UNIQUE,
 full_name VARCHAR(100) NOT NULL,
 email VARCHAR(120) NOT NULL UNIQUE,
 phone VARCHAR(20) NOT NULL UNIQUE,
 address VARCHAR(255),
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_customer_user FOREIGN KEY(user_id) REFERENCES users(user_id)
);

CREATE TABLE accounts (
 account_id BIGINT PRIMARY KEY AUTO_INCREMENT,
 customer_id BIGINT NOT NULL,
 account_number VARCHAR(20) NOT NULL UNIQUE,
 account_type ENUM('SAVINGS','CURRENT') NOT NULL,
 balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
 status ENUM('ACTIVE','FROZEN','CLOSED') NOT NULL DEFAULT 'ACTIVE',
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CONSTRAINT fk_account_customer FOREIGN KEY(customer_id) REFERENCES customers(customer_id),
 CONSTRAINT chk_balance CHECK(balance >= 0)
);

CREATE TABLE transactions (
 transaction_id BIGINT PRIMARY KEY AUTO_INCREMENT,
 reference_no VARCHAR(40) NOT NULL UNIQUE,
 from_account BIGINT NULL,
 to_account BIGINT NULL,
 transaction_type ENUM('DEPOSIT','WITHDRAWAL','TRANSFER') NOT NULL,
 amount DECIMAL(15,2) NOT NULL,
 status ENUM('SUCCESS','FLAGGED','FAILED') NOT NULL,
 description VARCHAR(255),
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(from_account) REFERENCES accounts(account_id),
 FOREIGN KEY(to_account) REFERENCES accounts(account_id),
 CONSTRAINT chk_amount CHECK(amount > 0)
);

CREATE TABLE fraud_alerts (
 alert_id BIGINT PRIMARY KEY AUTO_INCREMENT,
 transaction_id BIGINT NOT NULL,
 account_id BIGINT NOT NULL,
 risk_score INT NOT NULL,
 reason VARCHAR(255) NOT NULL,
 alert_status ENUM('OPEN','REVIEWED','CLEARED') NOT NULL DEFAULT 'OPEN',
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(transaction_id) REFERENCES transactions(transaction_id),
 FOREIGN KEY(account_id) REFERENCES accounts(account_id)
);

CREATE TABLE audit_logs (
 log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
 username VARCHAR(50) NOT NULL,
 action VARCHAR(80) NOT NULL,
 details VARCHAR(500),
 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create an administrator through the application's registration screen,
-- then change the role to ADMIN:
-- UPDATE users SET role='ADMIN' WHERE username='your_admin_username';
