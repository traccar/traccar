-- -------------------------------------
-- Create Schema
-- -------------------------------------

CREATE SCHEMA IF NOT EXISTS traccar_db
    DEFAULT CHARACTER SET = 'utf8'
    DEFAULT COLLATE = 'utf8_general_ci';

USE traccar_db;

-- -------------------------------------
-- Create Tables
-- -------------------------------------

CREATE TABLE application_settings (
    id SERIAL,
    registrationenabled BOOLEAN NOT NULL
);

CREATE TABLE devices (
    id SERIAL,
    name VARCHAR(255) DEFAULT NULL,
    uniqueid VARCHAR(255) DEFAULT NULL,
    latestposition_id BIGINT UNSIGNED
);

CREATE TABLE positions (
    id SERIAL,
    address VARCHAR(255) DEFAULT NULL,
    altitude REAL,
    course REAL,
    latitude REAL,
    longitude REAL,
    other VARCHAR(1024) DEFAULT NULL,
    power REAL,
    speed REAL,
    time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    device_id BIGINT UNSIGNED,
    valid BOOLEAN DEFAULT true
);

CREATE TABLE users (
    id SERIAL,
    admin BOOLEAN,
    login VARCHAR(255) DEFAULT NULL,
    password VARCHAR(255) DEFAULT NULL,
    usersettings_id BIGINT UNSIGNED
);

CREATE TABLE users_devices (
    users_id BIGINT UNSIGNED NOT NULL,
    devices_id BIGINT UNSIGNED NOT NULL
);

CREATE TABLE user_settings (
    id SERIAL,
    speedunit VARCHAR(255) DEFAULT NULL
);

-- -------------------------------------
-- Create keys
-- -------------------------------------

ALTER TABLE application_settings
    ADD CONSTRAINT app_set_pkey
    PRIMARY KEY (id);

ALTER TABLE devices
    ADD CONSTRAINT device_pkey
    PRIMARY KEY (id);

ALTER TABLE positions
    ADD CONSTRAINT position_pkey
    PRIMARY KEY (id);

ALTER TABLE users
    ADD CONSTRAINT users_pkey
    PRIMARY KEY (id);

ALTER TABLE users_devices
    ADD CONSTRAINT no_duplicate_pk
    PRIMARY KEY(users_id, devices_id); 

ALTER TABLE users_devices
    ADD CONSTRAINT no_duplicate_unique
    UNIQUE (users_id, devices_id);

ALTER TABLE user_settings
    ADD CONSTRAINT users_setting_pkey
    PRIMARY KEY (id);


-- -------------------------------------
-- Create foreign keys
-- -------------------------------------

ALTER TABLE devices
    ADD CONSTRAINT devices_fkey
    FOREIGN KEY (latestposition_id)
    REFERENCES positions(id)
    ON UPDATE CASCADE
    ON DELETE CASCADE;

ALTER TABLE positions
    ADD CONSTRAINT positions_fkey
    FOREIGN KEY (device_id)
    REFERENCES devices(id);

ALTER TABLE users
    ADD CONSTRAINT users_fkey
    FOREIGN KEY (usersettings_id)
    REFERENCES user_settings(id)
    ON UPDATE CASCADE
    ON DELETE CASCADE;

ALTER TABLE users_devices
    ADD CONSTRAINT users_devices_fkey1
    FOREIGN KEY (users_id)
    REFERENCES users(id)
    ON UPDATE CASCADE
    ON DELETE CASCADE;

ALTER TABLE users_devices
    ADD CONSTRAINT users_devices_fkey2
    FOREIGN KEY (devices_id)
    REFERENCES devices(id)
    ON UPDATE CASCADE
    ON DELETE CASCADE;

-- -------------------------------------
-- Create User
-- -------------------------------------

CREATE USER 'traccar_user'@'%';
SET PASSWORD FOR 'traccar_user' = PASSWORD('traccar-is-awesome');

GRANT SELECT, INSERT, UPDATE 
    ON TABLE traccar_db.*
    TO 'traccar_user'@'%';

