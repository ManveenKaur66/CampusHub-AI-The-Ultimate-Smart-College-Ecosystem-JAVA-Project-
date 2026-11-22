CREATE DATABASE IF NOT EXISTS campushub;
USE campushub;

CREATE TABLE IF NOT EXISTS users (
    email VARCHAR(150) PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    password_hash CHAR(64) NOT NULL,
    role ENUM('STUDENT', 'FACULTY') NOT NULL
);

CREATE TABLE IF NOT EXISTS students (
    roll_number VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    account_email VARCHAR(150) NULL UNIQUE,
    CONSTRAINT fk_student_account
        FOREIGN KEY (account_email) REFERENCES users(email)
        ON UPDATE CASCADE ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS profiles (
    profile_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    department VARCHAR(100) NOT NULL,
    role ENUM('STUDENT', 'FACULTY') NOT NULL,
    CONSTRAINT fk_profile_user
        FOREIGN KEY (email) REFERENCES users(email)
        ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS assignments (
    assignment_id INT AUTO_INCREMENT PRIMARY KEY,
    owner_email VARCHAR(150) NOT NULL,
    created_by VARCHAR(150),
    title VARCHAR(150) NOT NULL,
    description TEXT,
    due_date DATE NOT NULL,
    status ENUM('PENDING', 'SUBMITTED') DEFAULT 'PENDING',
    CONSTRAINT fk_assignment_owner
        FOREIGN KEY (owner_email) REFERENCES users(email)
        ON UPDATE CASCADE ON DELETE CASCADE,
    CONSTRAINT fk_assignment_creator
        FOREIGN KEY (created_by) REFERENCES users(email)
        ON UPDATE CASCADE ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS events (
    event_id INT AUTO_INCREMENT PRIMARY KEY,
    event_name VARCHAR(150) NOT NULL,
    category VARCHAR(60) NOT NULL,
    venue VARCHAR(150) NOT NULL,
    event_date DATE NOT NULL
);

CREATE TABLE IF NOT EXISTS registrations (
    registration_id INT AUTO_INCREMENT PRIMARY KEY,
    event_id INT NOT NULL,
    user_email VARCHAR(150) NOT NULL,
    UNIQUE KEY uq_event_user (event_id, user_email),
    CONSTRAINT fk_registration_event
        FOREIGN KEY (event_id) REFERENCES events(event_id) ON DELETE CASCADE,
    CONSTRAINT fk_registration_user
        FOREIGN KEY (user_email) REFERENCES users(email)
        ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS notifications (
    notification_id INT AUTO_INCREMENT PRIMARY KEY,
    recipient_email VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_notification_user
        FOREIGN KEY (recipient_email) REFERENCES users(email)
        ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE INDEX idx_assignment_due ON assignments(due_date, status);
CREATE INDEX idx_event_date ON events(event_date);
CREATE INDEX idx_notification_unread
    ON notifications(recipient_email, is_read);
