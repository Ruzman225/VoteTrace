USE votetrace;

CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(60) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS activity_log (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    username VARCHAR(100) NOT NULL,
    action VARCHAR(40) NOT NULL,
    outcome VARCHAR(24) NOT NULL,
    file_name VARCHAR(255) NULL,
    details TEXT NULL,
    event_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_activity_log_event_time (event_time),
    INDEX idx_activity_log_user_time (user_id, event_time)
) ENGINE=InnoDB;

GRANT SELECT, INSERT ON votetrace.app_user TO 'votetrace_app'@'localhost';
GRANT INSERT ON votetrace.activity_log TO 'votetrace_app'@'localhost';
FLUSH PRIVILEGES;
