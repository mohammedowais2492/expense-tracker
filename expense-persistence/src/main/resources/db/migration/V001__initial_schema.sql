
-- V001__initial_schema.sql
-- Initial database schema for ExpenseTracker.

-- ============================================================
-- User
-- ============================================================

CREATE TABLE User (
    UserId INTEGER PRIMARY KEY,
    FirstName TEXT NOT NULL,
    LastName TEXT,

    CONSTRAINT chk_user_first_name
        CHECK (length(trim(FirstName)) > 0)
);


-- ============================================================
-- ExpenseProfile
-- ============================================================

CREATE TABLE ExpenseProfile (
    ProfileId INTEGER PRIMARY KEY,
    UserId INTEGER NOT NULL,
    ProfileName TEXT NOT NULL COLLATE NOCASE,

    CONSTRAINT fk_expense_profile_user
        FOREIGN KEY (UserId)
        REFERENCES User(UserId)
        ON DELETE RESTRICT,

    CONSTRAINT uq_expense_profile_user
        UNIQUE (UserId),

    CONSTRAINT chk_expense_profile_name
        CHECK (length(trim(ProfileName)) > 0)
);


-- ============================================================
-- Category
-- ============================================================

CREATE TABLE Category (
    CategoryId INTEGER PRIMARY KEY,
    Name TEXT NOT NULL COLLATE NOCASE,
    Type TEXT NOT NULL,
    IsActive INTEGER NOT NULL DEFAULT 1,

    CONSTRAINT chk_category_name
        CHECK (length(trim(Name)) > 0),

    CONSTRAINT chk_category_type
        CHECK (Type IN ('INCOME', 'EXPENSE')),

    CONSTRAINT chk_category_active
        CHECK (IsActive IN (0, 1)),

    CONSTRAINT uq_category_name_type
        UNIQUE (Name, Type),

    CONSTRAINT uq_category_id_type
        UNIQUE (CategoryId, Type)
);


-- ============================================================
-- FinancialTransaction
-- ============================================================

CREATE TABLE FinancialTransaction (
    TransactionId INTEGER PRIMARY KEY,
    ProfileId INTEGER NOT NULL,
    CategoryId INTEGER NOT NULL,
    Type TEXT NOT NULL,
    Amount INTEGER NOT NULL,
    Description TEXT,
    TransactionDate TEXT NOT NULL,
    Necessity TEXT,

    CONSTRAINT fk_transaction_profile
        FOREIGN KEY (ProfileId)
        REFERENCES ExpenseProfile(ProfileId)
        ON DELETE CASCADE,

    CONSTRAINT fk_transaction_category_type
        FOREIGN KEY (CategoryId, Type)
        REFERENCES Category(CategoryId, Type)
        ON DELETE RESTRICT,

    CONSTRAINT chk_transaction_type
        CHECK (Type IN ('INCOME', 'EXPENSE')),

    CONSTRAINT chk_transaction_amount
        CHECK (typeof(Amount) = 'integer' AND Amount > 0),

    CONSTRAINT chk_transaction_necessity
        CHECK (
            (
                Type = 'EXPENSE'
                AND Necessity IS NOT NULL
                AND Necessity IN ('NECESSARY', 'UNNECESSARY')
            )
            OR
            (
                Type = 'INCOME'
                AND Necessity IS NULL
            )
        ),

    CONSTRAINT chk_transaction_date
        CHECK (
            TransactionDate GLOB
            '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'
        )
);


-- ============================================================
-- Indexes
-- ============================================================

-- Supports profile-scoped transaction queries ordered by
-- transaction date and transaction ID (newest first).
-- SQLite can also traverse this index in reverse.

CREATE INDEX idx_transaction_profile_date_id
ON FinancialTransaction (
    ProfileId,
    TransactionDate DESC,
    TransactionId DESC
);
