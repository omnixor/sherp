CREATE DATABASE IF NOT EXISTS smart_hostel;
USE smart_hostel;

CREATE TABLE IF NOT EXISTS students (
    student_id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    course VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(254) NULL,
    room_number INT NOT NULL,
    password_hash VARCHAR(255) NULL,
    UNIQUE KEY uq_students_email (email)
);

CREATE TABLE IF NOT EXISTS rooms (
    room_number INT PRIMARY KEY,
    capacity INT NOT NULL,
    occupied INT NOT NULL DEFAULT 0,
    CONSTRAINT chk_room_capacity CHECK (capacity > 0)
);

CREATE TABLE IF NOT EXISTS complaints (
    complaint_id INT PRIMARY KEY AUTO_INCREMENT,
    student_id INT NOT NULL,
    category VARCHAR(40) NOT NULL DEFAULT 'Other',
    title VARCHAR(120) NOT NULL DEFAULT 'Complaint',
    description VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'Pending',
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolution_note VARCHAR(1000) NULL,
    CONSTRAINT fk_complaints_student FOREIGN KEY (student_id) REFERENCES students(student_id)
);

CREATE TABLE IF NOT EXISTS room_allocations (
    allocation_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id INT NOT NULL,
    room_number INT NOT NULL,
    allocated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    vacated_at TIMESTAMP NULL,
    active_student_id INT GENERATED ALWAYS AS
        (CASE WHEN vacated_at IS NULL THEN student_id ELSE NULL END) STORED,
    UNIQUE KEY uq_room_allocations_active_student (active_student_id),
    INDEX ix_room_allocations_student (student_id, allocated_at),
    INDEX ix_room_allocations_room (room_number, vacated_at),
    CONSTRAINT fk_allocations_student FOREIGN KEY (student_id) REFERENCES students(student_id),
    CONSTRAINT fk_allocations_room FOREIGN KEY (room_number) REFERENCES rooms(room_number)
);

CREATE TABLE IF NOT EXISTS complaint_status_history (
    history_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    complaint_id INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    note VARCHAR(1000) NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX ix_complaint_history (complaint_id, changed_at),
    CONSTRAINT fk_complaint_history_complaint FOREIGN KEY (complaint_id)
        REFERENCES complaints(complaint_id)
);

CREATE TABLE IF NOT EXISTS notifications (
    notification_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    student_id INT NOT NULL,
    message VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX ix_notifications_student (student_id, is_read),
    CONSTRAINT fk_notifications_student FOREIGN KEY (student_id) REFERENCES students(student_id)
);

CREATE TABLE IF NOT EXISTS admin (
    admin_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS security_audit_log (
    event_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor_user_id INT NULL,
    actor_role VARCHAR(32) NOT NULL,
    action VARCHAR(64) NOT NULL,
    entity_type VARCHAR(32) NULL,
    entity_id VARCHAR(100) NULL,
    subject VARCHAR(100) NULL,
    outcome VARCHAR(20) NOT NULL,
    details VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX ix_audit_created_at (created_at),
    INDEX ix_audit_login_rate (action, actor_role, subject, created_at)
);
