DELIMITER $$
CREATE PROCEDURE InsertTestUsers()
BEGIN
    DECLARE i INT DEFAULT 1;
    WHILE i <= 1000 DO
        INSERT IGNORE INTO users (user_id, email, password, name)
        VALUES (UUID(), CONCAT('user', i, '@example.com'), '$2a$10$dXJ3SWoGD7Y759vA1OBMse7Fi7W7QKBjY7B6M62aT81q5Zc8M15oG', CONCAT('user', i));
        SET i = i + 1;
    END WHILE;
END$$
DELIMITER ;
CALL InsertTestUsers();
DROP PROCEDURE InsertTestUsers;
