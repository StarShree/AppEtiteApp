-- ==========================================================
-- AppEtite: SQL Script for Separate User Role Tables
-- Target: Google Cloud SQL (MySQL 8.0) | Database: AppEtiteDB
--
-- This script creates 4 separate tables:
--  1. campus_users   (Students, Staff & Campus Dining Patrons)
--  2. canteen_staff   (Kitchen Operators, Chefs & Vendors)
--  3. college_admins  (Campus Dining Deans & Administrators)
--  4. super_admins    (Platform & Multi-Campus Super Admins)
--
-- Plus:
--  - Creates the unified view `vw_all_system_users`
--  - Migrates existing data from legacy `users` table (if present)
--  - Seeds default demonstration records for each role
-- ==========================================================

USE AppEtiteDB;

-- ----------------------------------------------------------
-- 1. TABLE: campus_users
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS campus_users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    id VARCHAR(64) UNIQUE,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone_number VARCHAR(32) NULL,
    college_id INT NULL,
    college_id_str VARCHAR(64) NULL,
    assigned_canteen_id VARCHAR(64) NULL,
    campus_id_number VARCHAR(64) NULL COMMENT 'Student roll or staff campus ID',
    wallet_balance DECIMAL(10, 2) NOT NULL DEFAULT 350.00,
    status ENUM('ACTIVE', 'SUSPENDED', 'PENDING_APPROVAL') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_campus_user_email (email),
    INDEX idx_campus_user_college (college_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------
-- 2. TABLE: canteen_staff
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS canteen_staff (
    staff_id INT AUTO_INCREMENT PRIMARY KEY,
    id VARCHAR(64) UNIQUE,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone_number VARCHAR(32) NULL,
    college_id INT NOT NULL,
    college_id_str VARCHAR(64) NULL,
    assigned_canteen_id VARCHAR(64) NOT NULL,
    designation VARCHAR(80) NOT NULL DEFAULT 'Kitchen Operator & Chef',
    status ENUM('ACTIVE', 'SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_canteen_staff_email (email),
    INDEX idx_canteen_staff_canteen (assigned_canteen_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------
-- 3. TABLE: college_admins
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS college_admins (
    admin_id INT AUTO_INCREMENT PRIMARY KEY,
    id VARCHAR(64) UNIQUE,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone_number VARCHAR(32) NULL,
    college_id INT NOT NULL,
    college_id_str VARCHAR(64) NULL,
    department VARCHAR(100) NOT NULL DEFAULT 'Campus Dining Administration',
    status ENUM('ACTIVE', 'SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_college_admin_email (email),
    INDEX idx_college_admin_college (college_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------
-- 4. TABLE: super_admins
-- ----------------------------------------------------------
CREATE TABLE IF NOT EXISTS super_admins (
    super_admin_id INT AUTO_INCREMENT PRIMARY KEY,
    id VARCHAR(64) UNIQUE,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    phone_number VARCHAR(32) NULL,
    permission_level VARCHAR(50) NOT NULL DEFAULT 'SUPER_ACCESS_ALL',
    status ENUM('ACTIVE', 'SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_super_admin_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------
-- 5. UNIFIED VIEW: vw_all_system_users
-- ----------------------------------------------------------
CREATE OR REPLACE VIEW vw_all_system_users AS
SELECT 
    user_id, id, name, email, password_hash, 'CUSTOMER' AS role, 'CAMPUS_USER' AS user_type,
    college_id, college_id_str, assigned_canteen_id, campus_id_number, phone_number, wallet_balance, status, created_at, updated_at
FROM campus_users
UNION ALL
SELECT 
    staff_id AS user_id, id, name, email, password_hash, 'KITCHEN_STAFF' AS role, 'CANTEEN_STAFF' AS user_type,
    college_id, college_id_str, assigned_canteen_id, NULL AS campus_id_number, phone_number, 0.00 AS wallet_balance, status, created_at, updated_at
FROM canteen_staff
UNION ALL
SELECT 
    admin_id AS user_id, id, name, email, password_hash, 'COLLEGE_ADMIN' AS role, 'COLLEGE_ADMIN' AS user_type,
    college_id, college_id_str, NULL AS assigned_canteen_id, NULL AS campus_id_number, phone_number, 0.00 AS wallet_balance, status, created_at, updated_at
FROM college_admins
UNION ALL
SELECT 
    super_admin_id AS user_id, id, name, email, password_hash, 'SUPER_ADMIN' AS role, 'SUPER_ADMIN' AS user_type,
    NULL AS college_id, NULL AS college_id_str, NULL AS assigned_canteen_id, NULL AS campus_id_number, phone_number, 0.00 AS wallet_balance, status, created_at, updated_at
FROM super_admins;

-- ----------------------------------------------------------
-- 6. DATA MIGRATION: Copy records from legacy `users` table (if exists)
-- ----------------------------------------------------------
INSERT IGNORE INTO campus_users (name, email, password_hash, college_id, status)
SELECT name, email, password_hash, college_id, 'ACTIVE' 
FROM users 
WHERE role IN ('CUSTOMER', 'STUDENT');

INSERT IGNORE INTO canteen_staff (name, email, password_hash, college_id, assigned_canteen_id, status)
SELECT name, email, password_hash, IFNULL(college_id, 1), 'canteen_1_1', 'ACTIVE'
FROM users 
WHERE role IN ('KITCHEN_STAFF', 'VENDOR');

INSERT IGNORE INTO college_admins (name, email, password_hash, college_id, status)
SELECT name, email, password_hash, IFNULL(college_id, 1), 'ACTIVE'
FROM users 
WHERE role = 'COLLEGE_ADMIN';

INSERT IGNORE INTO super_admins (name, email, password_hash, status)
SELECT name, email, password_hash, 'ACTIVE'
FROM users 
WHERE role IN ('SUPER_ADMIN', 'ROLE_ORDER', 'ROLE_CUSTOMER_DETAIL', 'ROLE_TRANSACTION');

-- ----------------------------------------------------------
-- 7. SEED VALUES FOR ALL 4 SEPARATE TABLES
-- ----------------------------------------------------------

-- Campus Users (Students & Campus Diners)
INSERT INTO campus_users (id, name, email, password_hash, phone_number, college_id, college_id_str, assigned_canteen_id, campus_id_number, wallet_balance, status)
VALUES 
('user_student_1', 'Alex Rivera', 'alex.rivera@campus.edu', 'Student@123', '+1 555-0199', 1, 'col_1', 'canteen_1_1', '2024-CS-042', 350.00, 'ACTIVE'),
('user_student_2', 'Alex Johnson', 'alex@student.imperial.edu', 'Student@123', '+1 555-0144', 2, 'col_2', 'canteen_2_1', '2023-EE-118', 280.00, 'ACTIVE'),
('user_student_3', 'Emily Watson', 'emily@student.stjude.edu', 'Student@123', '+1 555-0177', 3, 'col_3', 'canteen_3_1', '2024-ME-077', 310.00, 'ACTIVE')
ON DUPLICATE KEY UPDATE name=VALUES(name), wallet_balance=VALUES(wallet_balance);

-- Canteen Staff (Kitchen Operators & Chefs)
INSERT INTO canteen_staff (id, name, email, password_hash, phone_number, college_id, college_id_str, assigned_canteen_id, designation, status)
VALUES 
('user_vendor_1', 'Chef Roberto', 'chef.roberto@bytebites.edu', 'Kitchen@123', '+1 555-0201', 1, 'col_1', 'canteen_1_1', 'Head Chef & Kitchen Operator', 'ACTIVE'),
('user_vendor_2', 'Chef Mario Rossi', 'kitchen.imperial@appetite.com', 'Kitchen@123', '+1 555-0202', 2, 'col_2', 'canteen_2_1', 'Executive Canteen Chef', 'ACTIVE'),
('user_vendor_3', 'Chef Gordon Patel', 'kitchen.stjude@appetite.com', 'Kitchen@123', '+1 555-0203', 3, 'col_3', 'canteen_3_1', 'Lead Line Cook & Prep Manager', 'ACTIVE')
ON DUPLICATE KEY UPDATE name=VALUES(name), assigned_canteen_id=VALUES(assigned_canteen_id);

-- College Admins (Institutional Administrators)
INSERT INTO college_admins (id, name, email, password_hash, phone_number, college_id, college_id_str, department, status)
VALUES 
('user_admin_1', 'Dean Harrison', 'dean.harrison@stanford.edu', 'Admin@123', '+1 555-0301', 1, 'col_1', 'Dean of Student Dining Services', 'ACTIVE'),
('user_admin_2', 'Prof. Robert Davis', 'admin.imperial@appetite.com', 'Admin@123', '+1 555-0302', 2, 'col_2', 'Canteen Oversight Committee', 'ACTIVE'),
('user_admin_3', 'Dr. Sarah Connor', 'admin.stjude@appetite.com', 'Admin@123', '+1 555-0303', 3, 'col_3', 'Director of Campus Facilities', 'ACTIVE')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Super Admins (Platform Super Administrators)
INSERT INTO super_admins (id, name, email, password_hash, phone_number, permission_level, status)
VALUES 
('user_super_1', 'Dr. Sarah Vance', 'sarah.vance@appetite.io', 'Admin@123', '+1 555-0401', 'SUPER_ACCESS_ALL', 'ACTIVE'),
('user_super_2', 'Chief System Administrator', 'superadmin@appetite.com', 'Admin@123', '+1 555-0402', 'SUPER_ACCESS_ALL', 'ACTIVE')
ON DUPLICATE KEY UPDATE name=VALUES(name);
