CREATE DATABASE IF NOT EXISTS votetrace
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'votetrace_app'@'localhost'
    IDENTIFIED BY 'root123';
