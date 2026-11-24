-- Library Management System Database Schema
-- Compatible with MySQL 8+. For PostgreSQL, change AUTO_INCREMENT -> SERIAL / IDENTITY
-- and adjust ENUM types to VARCHAR + CHECK constraints if desired.

CREATE DATABASE IF NOT EXISTS library_db;
USE library_db;

-- ============================================================
-- Table: books
-- ============================================================
CREATE TABLE IF NOT EXISTS books (
    book_id       INT AUTO_INCREMENT PRIMARY KEY,
    isbn          VARCHAR(20)  NOT NULL UNIQUE,
    title         VARCHAR(255) NOT NULL,
    author        VARCHAR(255) NOT NULL,
    genre         VARCHAR(100),
    publisher     VARCHAR(150),
    publish_year  INT,
    total_copies  INT NOT NULL DEFAULT 1,
    available_copies INT NOT NULL DEFAULT 1,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- Table: members
-- ============================================================
CREATE TABLE IF NOT EXISTS members (
    member_id     INT AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(150) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    phone         VARCHAR(30),
    address       VARCHAR(255),
    membership_date DATE DEFAULT (CURRENT_DATE),
    status        ENUM('ACTIVE', 'SUSPENDED', 'EXPIRED') NOT NULL DEFAULT 'ACTIVE'
);

-- ============================================================
-- Table: loans  (a "loan" = one borrow transaction)
-- ============================================================
CREATE TABLE IF NOT EXISTS loans (
    loan_id       INT AUTO_INCREMENT PRIMARY KEY,
    book_id       INT NOT NULL,
    member_id     INT NOT NULL,
    loan_date     DATE NOT NULL DEFAULT (CURRENT_DATE),
    due_date      DATE NOT NULL,
    return_date   DATE NULL,
    status        ENUM('BORROWED', 'RETURNED', 'OVERDUE') NOT NULL DEFAULT 'BORROWED',
    fine_amount   DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    CONSTRAINT fk_loan_book   FOREIGN KEY (book_id)   REFERENCES books(book_id)   ON DELETE CASCADE,
    CONSTRAINT fk_loan_member FOREIGN KEY (member_id) REFERENCES members(member_id) ON DELETE CASCADE
);

CREATE INDEX idx_loans_status ON loans(status);
CREATE INDEX idx_books_title  ON books(title);
CREATE INDEX idx_books_author ON books(author);

-- ============================================================
-- Sample seed data (optional)
-- ============================================================
INSERT INTO books (isbn, title, author, genre, publisher, publish_year, total_copies, available_copies) VALUES
('978-0134685991', 'Effective Java', 'Joshua Bloch', 'Programming', 'Addison-Wesley', 2018, 3, 3),
('978-0596007126', 'Head First Design Patterns', 'Eric Freeman', 'Programming', 'O''Reilly', 2004, 2, 2),
('978-0141439518', 'Pride and Prejudice', 'Jane Austen', 'Fiction', 'Penguin Classics', 1813, 4, 4);

INSERT INTO members (full_name, email, phone, address) VALUES
('Alice Johnson', 'alice.johnson@example.com', '555-0101', '12 Maple St'),
('Bob Smith', 'bob.smith@example.com', '555-0102', '48 Oak Ave');
