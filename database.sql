CREATE DATABASE IF NOT EXISTS smart_hostel;
USE smart_hostel;

CREATE TABLE IF NOT EXISTS students (
    student_id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    course VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    room_number INT NOT NULL
);

CREATE TABLE IF NOT EXISTS rooms (
    room_number INT PRIMARY KEY,
    capacity INT NOT NULL,
    occupied INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS complaints (
    complaint_id INT PRIMARY KEY AUTO_INCREMENT,
    student_id INT NOT NULL,
    description VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'Pending'
);

CREATE TABLE IF NOT EXISTS admin (
    admin_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL
);

INSERT IGNORE INTO admin (username, password) VALUES ('admin', 'admin123');
