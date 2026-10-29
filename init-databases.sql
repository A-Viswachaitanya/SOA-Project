-- ============================================
-- PostgreSQL Database Setup Script
-- PS009: RecurringFlex Subscription Management
-- ============================================

-- Run this script as the postgres superuser
-- psql -U postgres -f init-databases.sql

-- Create databases
CREATE DATABASE user_db;
CREATE DATABASE subscription_db;
CREATE DATABASE payment_db;

-- Note: Tables will be auto-created by Spring Boot JPA (ddl-auto=update)
-- The following is for reference only:

-- ============================================
-- user_db tables (managed by User Service)
-- ============================================
-- users (id, username, email, password, role, created_at)

-- ============================================
-- subscription_db tables (managed by Subscription Service)
-- ============================================
-- plans (id, name, description, price, duration_in_days, active, created_at)
-- subscriptions (id, user_id, plan_id, start_date, end_date, status, auto_renew, created_at)

-- ============================================
-- payment_db tables (managed by Payment Service)
-- ============================================
-- payments (id, subscription_id, user_id, amount, status, transaction_ref, payment_date)
