-- 创建数据库
CREATE DATABASE IF NOT EXISTS hanye_take_out CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE hanye_take_out;

-- 创建员工表
CREATE TABLE IF NOT EXISTS employee (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    account VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    age INT,
    gender INT,
    pic VARCHAR(255),
    status INT DEFAULT 1,
    create_user INT,
    update_user INT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 创建用户表
CREATE TABLE IF NOT EXISTS user (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50),
    openid VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20),
    gender INT,
    id_number VARCHAR(20),
    pic VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 创建地址簿表
CREATE TABLE IF NOT EXISTS address_book (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    consignee VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    province_code VARCHAR(20),
    province_name VARCHAR(50),
    city_code VARCHAR(20),
    city_name VARCHAR(50),
    district_code VARCHAR(20),
    district_name VARCHAR(50),
    detail VARCHAR(255) NOT NULL,
    is_default INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id)
);

-- 创建分类表
CREATE TABLE IF NOT EXISTS category (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    type INT NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 创建菜品表
CREATE TABLE IF NOT EXISTS dish (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    category_id INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    description VARCHAR(255),
    image VARCHAR(255),
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES category(id)
);

-- 创建菜品口味表
CREATE TABLE IF NOT EXISTS dish_flavor (
    id INT PRIMARY KEY AUTO_INCREMENT,
    dish_id INT NOT NULL,
    name VARCHAR(50) NOT NULL,
    value VARCHAR(255) NOT NULL,
    FOREIGN KEY (dish_id) REFERENCES dish(id)
);

-- 创建套餐表
CREATE TABLE IF NOT EXISTS setmeal (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    category_id INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    description VARCHAR(255),
    image VARCHAR(255),
    status INT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES category(id)
);

-- 创建套餐菜品表
CREATE TABLE IF NOT EXISTS setmeal_dish (
    id INT PRIMARY KEY AUTO_INCREMENT,
    setmeal_id INT NOT NULL,
    dish_id INT NOT NULL,
    name VARCHAR(50) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    copies INT NOT NULL,
    FOREIGN KEY (setmeal_id) REFERENCES setmeal(id),
    FOREIGN KEY (dish_id) REFERENCES dish(id)
);

-- 创建购物车表
CREATE TABLE IF NOT EXISTS cart (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    dish_id INT,
    setmeal_id INT,
    dish_flavor VARCHAR(255),
    number INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (dish_id) REFERENCES dish(id),
    FOREIGN KEY (setmeal_id) REFERENCES setmeal(id)
);

-- 创建订单表
CREATE TABLE IF NOT EXISTS `order` (
    id INT PRIMARY KEY AUTO_INCREMENT,
    number VARCHAR(50) NOT NULL UNIQUE,
    status INT NOT NULL,
    user_id INT NOT NULL,
    address_book_id INT NOT NULL,
    order_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    checkout_time DATETIME,
    pay_method INT,
    pay_status INT DEFAULT 0,
    amount DECIMAL(10,2) NOT NULL,
    remark VARCHAR(255),
    phone VARCHAR(20) NOT NULL,
    address VARCHAR(255) NOT NULL,
    consignee VARCHAR(50) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (address_book_id) REFERENCES address_book(id)
);

-- 创建订单详情表
CREATE TABLE IF NOT EXISTS order_detail (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    dish_id INT,
    setmeal_id INT,
    dish_flavor VARCHAR(255),
    number INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    name VARCHAR(50) NOT NULL,
    image VARCHAR(255),
    FOREIGN KEY (order_id) REFERENCES `order`(id),
    FOREIGN KEY (dish_id) REFERENCES dish(id),
    FOREIGN KEY (setmeal_id) REFERENCES setmeal(id)
);

-- 插入默认员工数据
INSERT INTO employee (name, account, password, phone, age, gender, status) 
VALUES ('超级管理员', 'cyh', 'e10adc3949ba59abbe56e057f20f883e', '13800138000', 25, 1, 1);

-- 插入默认分类数据
INSERT INTO category (name, type, sort, status) 
VALUES ('热销菜品', 1, 1, 1),
       ('套餐', 2, 1, 1);
