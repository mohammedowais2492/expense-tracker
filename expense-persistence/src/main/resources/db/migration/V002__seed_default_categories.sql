
-- V002__seed_default_categories.sql
-- Initial expense categories for ExpenseTracker.
-- All categories are active by default.

INSERT INTO Category (Name, Type, IsActive)
VALUES
    ('Rent', 'EXPENSE', 1),
    ('Car Loan Payment', 'EXPENSE', 1),
    ('Utilities', 'EXPENSE', 1),
    ('Subscriptions', 'EXPENSE', 1),
    ('Restaurant', 'EXPENSE', 1),
    ('Gas', 'EXPENSE', 1),
    ('Cab', 'EXPENSE', 1),
    ('Food Order', 'EXPENSE', 1),
    ('Groceries', 'EXPENSE', 1),
    ('Miscellaneous', 'EXPENSE', 1),
    ('Insurance', 'EXPENSE', 1),
    ('Car Maintenance', 'EXPENSE', 1),
    ('Shopping', 'EXPENSE', 1),
    ('Healthcare', 'EXPENSE', 1),
    ('Entertainment', 'EXPENSE', 1),
    ('Travel', 'EXPENSE', 1);
