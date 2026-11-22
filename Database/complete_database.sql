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

USE campushub;

INSERT INTO students (roll_number, name) VALUES
('2510990910', 'Amol'),
('2510990864', 'Akul Gupta'),
('2510990211', 'Mathely'),
('2510990898', 'Vaibhavi'),
('2510990504', 'Rakshita'),
('2510990652', 'Mehak Sharma'),
('2510990787', 'Vanshika'),
('2510990226', 'Tanish pal singh'),
('2510990306', 'Dhiren Sharma'),
('2510991386', 'Jaupdeep Singh'),
('2510991427', 'Krishna'),
('2510990819', 'Aryan Sharma'),
('2510990842', 'Prabhnoor Singh'),
('2510991041', 'Hemang Tandon'),
('2510991046', 'Saksham Chopra'),
('2510991087', 'Droan Manhas'),
('2510990589', 'Mannat Oberoi'),
('2510990873', 'Eknoor Singh'),
('2510990248', 'Shivansh Thakur'),
('2510990207', 'Puneet Rajesh kewlani'),
('2510990384', 'Sachin Sharma'),
('2510990507', 'Krishna Kirola'),
('2510990512', 'Krish'),
('2510991359', 'Jayant Khanna'),
('2510991437', 'Amrita Mandal'),
('2510991445', 'Arshia Sood'),
('2510991344', 'Rakesh Kumar'),
('2510990270', 'Ishu Shukla'),
('2510991066', 'Anusha Garg'),
('2510990458', 'Parul'),
('2510990471', 'Kangna Dhiman'),
('2510990475', 'Aryan patial'),
('2510991082', 'Parth Budhiraja'),
('2510991368', 'Vrinda Kalra'),
('2510991398', 'Gunjot Kaur'),
('2510991458', 'Siddhika'),
('2510991203', 'Rajinder Kaur'),
('2510991309', 'Prachi Soni'),
('2510991310', 'Nitin Sharma'),
('2520991315', 'Utkarsh singh'),
('2510990571', 'Jaskaran Singh'),
('2510990660', 'Nirjit Singh'),
('2510990669', 'Soumildeep Singh'),
('2510990902', 'Niharika Gupta'),
('2510991372', 'Nandini Aggarwal'),
('2510990967', 'Khushi'),
('2510990730', 'Simranjit Kaur'),
('2510990752', 'Nibhi khanna'),
('2510990887', 'PALAK VERMA'),
('2510991081', 'Lubhav Bansal'),
('2510990088', 'Shivam bhatt'),
('2510990003', 'Rakshit Awasthi'),
('2510990141', 'Hitain Singla'),
('2510991395', 'Bhavya kamal'),
('2510990614', 'Mannatbir Singh'),
('2510990624', 'Manveen Kaur'),
('2510990629', 'Brhamjyot Kaur'),
('2510990695', 'Parteek Washist'),
('2510991165', 'Nrip Aggarwal'),
('2510991259', 'Saksham Singla'),
('2510991320', 'Nitish Raj'),
('2510991215', 'Somansh Walia'),
('2510990189', 'Keerat Gujral'),
('2510990315', 'Tanvi'),
('2510990899', 'Chaitali Mahajan'),
('2510990844', 'Akshit Sharma'),
('2510991047', 'Prachi Bhardwaj'),
('2510990138', 'Kamalpreet kaur'),
('2510990156', 'Kushagra Jha'),
('2510990753', 'Sharanpreet Kaur'),
('2510990756', 'Saksham jain'),
('2510990621', 'Krishna Walia'),
('2510991016', 'Mohd Azeem')
ON DUPLICATE KEY UPDATE name = VALUES(name);
