CREATE TABLE IF NOT EXISTS `like`
(
    like_id    VARCHAR(36) PRIMARY KEY,
    entry_id   INT          NOT NULL,
    like_at    TIMESTAMP    NOT NULL,
    ip_address VARCHAR(128) NOT NULL,
    INDEX like_at (like_at),
    UNIQUE entry_id_and_like_ip_address (entry_id, ip_address)
);