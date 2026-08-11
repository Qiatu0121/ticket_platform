-- 校园活动抢票平台 建库建表脚本(源自 README)
CREATE DATABASE IF NOT EXISTS `ticket_platform` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `ticket_platform`;

CREATE TABLE IF NOT EXISTS `user` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(50) NOT NULL UNIQUE,
  phone VARCHAR(20) NOT NULL UNIQUE,
  nickname VARCHAR(50),
  password_hash VARCHAR(100) NOT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'USER',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 存量库升级（已建过 user 表的执行下面四条）：
-- ALTER TABLE `user` ADD COLUMN username VARCHAR(50) NULL UNIQUE AFTER id;
-- ALTER TABLE `user` ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';
-- UPDATE `user` SET username = nickname WHERE username IS NULL;   -- 老用户直接用昵称当用户名
-- （若 nickname 为空或重复，再兜底：UPDATE `user` SET username = CONCAT('user_', id) WHERE username IS NULL;）

CREATE TABLE IF NOT EXISTS `activity` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  start_time DATETIME,
  location VARCHAR(200),
  detail TEXT,
  status TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `activity_ticket` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  activity_id BIGINT NOT NULL,
  name VARCHAR(50),
  price DECIMAL(10,2) DEFAULT 0,
  total_stock INT DEFAULT 0,
  stock INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `order` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(40) NOT NULL,
  user_id BIGINT NOT NULL,
  activity_id BIGINT NOT NULL,
  ticket_id BIGINT NOT NULL,
  `count` INT DEFAULT 1,
  amount DECIMAL(10,2),
  status TINYINT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  pay_time DATETIME,
  UNIQUE KEY uk_user_activity (user_id, activity_id)
);

CREATE TABLE IF NOT EXISTS `order_log` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  from_status VARCHAR(20),
  to_status VARCHAR(20) NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
