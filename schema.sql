-- ==========================================================
-- AppEtite: Smart Campus Dining & Canteen Management System
-- Database Schema for Google Cloud SQL (MySQL 8.0)
-- Host: 34.100.184.249 | Database: AppEtiteDB
--
-- SEPARATE USER ROLE TABLES:
-- 1. campus_users   (Students, Faculty & Campus Dining Members)
-- 2. canteen_staff   (Kitchen Chefs & Canteen Operators)
-- 3. college_admins  (Campus & Institutional Dining Deans)
-- 4. super_admins    (Platform & Multi-Campus Super Admins)
-- ==========================================================

-- 1. COLLEGES TABLE
CREATE TABLE IF NOT EXISTS colleges (
    college_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    location VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. CANTEENS TABLE (Multiple canteens per college)
CREATE TABLE IF NOT EXISTS canteens (
    id VARCHAR(64) PRIMARY KEY,
    college_id VARCHAR(64) NOT NULL,
    name VARCHAR(150) NOT NULL,
    location VARCHAR(255) NULL,
    operating_hours VARCHAR(100) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================================
-- SEPARATE USER TABLES
-- ==========================================================

-- 3A. CAMPUS USERS TABLE (Students & Campus Dining Customers)
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
    campus_id_number VARCHAR(64) NULL,
    wallet_balance DECIMAL(10, 2) NOT NULL DEFAULT 350.00,
    status ENUM('ACTIVE', 'SUSPENDED', 'PENDING_APPROVAL') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_campus_user_college
        FOREIGN KEY (college_id) REFERENCES colleges(college_id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3B. CANTEEN STAFF TABLE (Kitchen Chefs, Cashiers & Canteen Vendors)
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
    CONSTRAINT fk_canteen_staff_college
        FOREIGN KEY (college_id) REFERENCES colleges(college_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3C. COLLEGE ADMINS TABLE (Institutional Dining Administrators & Deans)
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
    CONSTRAINT fk_college_admin_college
        FOREIGN KEY (college_id) REFERENCES colleges(college_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3D. SUPER ADMINS TABLE (Platform Super Administrators)
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
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. UNIFIED VIEW FOR RETRO-COMPATIBILITY AND CROSS-ROLE AUDITING
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

-- 5. MENU ITEMS TABLE
CREATE TABLE IF NOT EXISTS menu_items (
    item_id INT AUTO_INCREMENT PRIMARY KEY,
    college_id INT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT NULL,
    price DECIMAL(10, 2) NOT NULL,
    category ENUM('Breakfast', 'Lunch', 'Beverages', 'Snacks', 'Fast Food', 'Meals', 'Healthy Bowls', 'Veg') NOT NULL,
    image_url VARCHAR(500) NULL,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_menu_college
        FOREIGN KEY (college_id) REFERENCES colleges (college_id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. ORDERS TABLE
CREATE TABLE IF NOT EXISTS orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    college_id INT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    token_number INT NOT NULL,
    order_status ENUM('Placed', 'Preparing', 'Ready', 'Completed', 'Cancelled') NOT NULL DEFAULT 'Placed',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_college
        FOREIGN KEY (college_id) REFERENCES colleges (college_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. ORDER ITEMS TABLE
CREATE TABLE IF NOT EXISTS order_items (
    order_item_id INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    item_id INT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    price DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id) REFERENCES orders (order_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_order_items_item
        FOREIGN KEY (item_id) REFERENCES menu_items (item_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ==========================================================
-- INDEXES FOR MAXIMUM QUERY PERFORMANCE
-- ==========================================================
CREATE INDEX IF NOT EXISTS idx_cu_email ON campus_users(email);
CREATE INDEX IF NOT EXISTS idx_cu_college ON campus_users(college_id);
CREATE INDEX IF NOT EXISTS idx_cs_email ON canteen_staff(email);
CREATE INDEX IF NOT EXISTS idx_cs_canteen ON canteen_staff(assigned_canteen_id);
CREATE INDEX IF NOT EXISTS idx_ca_email ON college_admins(email);
CREATE INDEX IF NOT EXISTS idx_ca_college ON college_admins(college_id);
CREATE INDEX IF NOT EXISTS idx_sa_email ON super_admins(email);

-- ==========================================================
-- INITIAL SEED VALUES FOR ALL 4 SEPARATE TABLES
-- ==========================================================

-- Colleges
INSERT IGNORE INTO colleges (college_id, name, location) VALUES
(1, 'Stanford University Campus', 'Main Campus, Building A'),
(2, 'Imperial College of Engineering', 'North Campus, Block A'),
(3, 'St. Jude Institute of Technology', 'South Campus, Central Plaza');

-- Canteens (2 Canteens per College)
INSERT IGNORE INTO canteens (id, college_id, name, location, operating_hours) VALUES
('canteen_1_1', 'col_1', 'Byte Bites Cyber Cafe', 'Ground Floor, Engineering Wing', '8:00 AM - 9:00 PM'),
('canteen_1_2', 'col_1', 'The Oval Bistro & Express', 'Central Quadrangle, Student Center', '7:30 AM - 10:00 PM'),
('canteen_2_1', 'col_2', 'Imperial North Food Court', 'North Campus, Block A Ground Floor', '8:00 AM - 8:30 PM'),
('canteen_2_2', 'col_2', 'Royal Roast & Juice Lounge', 'East Wing, Science Complex', '8:30 AM - 9:30 PM'),
('canteen_3_1', 'col_3', 'Central Plaza Food Pavilion', 'South Campus, Central Plaza Level 1', '7:30 AM - 9:00 PM'),
('canteen_3_2', 'col_3', 'Tech Hub Cafe & Grille', 'Innovation & IT Tower, Level 2', '8:00 AM - 10:30 PM');

-- 1. SEED CAMPUS USERS (Separate Table: campus_users)
INSERT IGNORE INTO campus_users (user_id, id, name, email, password_hash, phone_number, college_id, college_id_str, assigned_canteen_id, campus_id_number, wallet_balance, status) VALUES
(1, 'user_student_1', 'Alex Rivera', 'alex.rivera@campus.edu', 'Student@123', '+1 555-0199', 1, 'col_1', 'canteen_1_1', '2024-CS-042', 350.00, 'ACTIVE'),
(2, 'user_student_2', 'Alex Johnson', 'alex@student.imperial.edu', 'Student@123', '+1 555-0144', 2, 'col_2', 'canteen_2_1', '2023-EE-118', 280.00, 'ACTIVE'),
(3, 'user_student_3', 'Emily Watson', 'emily@student.stjude.edu', 'Student@123', '+1 555-0177', 3, 'col_3', 'canteen_3_1', '2024-ME-077', 310.00, 'ACTIVE');

-- 2. SEED CANTEEN STAFF (Separate Table: canteen_staff)
INSERT IGNORE INTO canteen_staff (staff_id, id, name, email, password_hash, phone_number, college_id, college_id_str, assigned_canteen_id, designation, status) VALUES
(1, 'user_vendor_1', 'Chef Roberto', 'chef.roberto@bytebites.edu', 'Kitchen@123', '+1 555-0201', 1, 'col_1', 'canteen_1_1', 'Head Chef & Kitchen Operator', 'ACTIVE'),
(2, 'user_vendor_2', 'Chef Mario Rossi', 'kitchen.imperial@appetite.com', 'Kitchen@123', '+1 555-0202', 2, 'col_2', 'canteen_2_1', 'Executive Canteen Chef', 'ACTIVE'),
(3, 'user_vendor_3', 'Chef Gordon Patel', 'kitchen.stjude@appetite.com', 'Kitchen@123', '+1 555-0203', 3, 'col_3', 'canteen_3_1', 'Lead Line Cook & Prep Manager', 'ACTIVE');

-- 3. SEED COLLEGE ADMINS (Separate Table: college_admins)
INSERT IGNORE INTO college_admins (admin_id, id, name, email, password_hash, phone_number, college_id, college_id_str, department, status) VALUES
(1, 'user_admin_1', 'Dean Harrison', 'dean.harrison@stanford.edu', 'Admin@123', '+1 555-0301', 1, 'col_1', 'Dean of Student Dining Services', 'ACTIVE'),
(2, 'user_admin_2', 'Prof. Robert Davis', 'admin.imperial@appetite.com', 'Admin@123', '+1 555-0302', 2, 'col_2', 'Canteen Oversight Committee', 'ACTIVE'),
(3, 'user_admin_3', 'Dr. Sarah Connor', 'admin.stjude@appetite.com', 'Admin@123', '+1 555-0303', 3, 'col_3', 'Director of Campus Facilities', 'ACTIVE');

-- 4. SEED SUPER ADMINS (Separate Table: super_admins)
INSERT IGNORE INTO super_admins (super_admin_id, id, name, email, password_hash, phone_number, permission_level, status) VALUES
(1, 'user_super_1', 'Dr. Sarah Vance', 'sarah.vance@appetite.io', 'Admin@123', '+1 555-0401', 'SUPER_ACCESS_ALL', 'ACTIVE'),
(2, 'user_super_2', 'Chief System Administrator', 'superadmin@appetite.com', 'Admin@123', '+1 555-0402', 'SUPER_ACCESS_ALL', 'ACTIVE');
