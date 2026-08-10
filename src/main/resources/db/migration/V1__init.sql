-- Flyway baseline migration: create users and time_off_request tables

CREATE TABLE IF NOT EXISTS "users" (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    role VARCHAR(50) NOT NULL DEFAULT 'EMPLOYEE',
    manager_id INTEGER NULL,
    is_email_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE IF NOT EXISTS time_off_request (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES "users"(id),
    type VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    partial_day VARCHAR(10),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    reviewed_by INTEGER NULL,
    reviewed_at TIMESTAMP WITH TIME ZONE NULL,
    manager_comment TEXT NULL
);
