-- -------------------------------------
-- Create Schema
-- -------------------------------------

CREATE SCHEMA IF NOT EXISTS  traccar_db;

SET SCHEMA  traccar_db;

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
    other VARCHAR(255) DEFAULT NULL,
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

ALTER TABLE users_devices
    ADD CONSTRAINT no_duplicate_pk
    PRIMARY KEY(users_id, devices_id); 

ALTER TABLE users_devices
    ADD CONSTRAINT no_duplicate_unique
    UNIQUE (users_id, devices_id);


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

CREATE USER  traccar_user  PASSWORD 'traccar-is-awesome';

GRANT SELECT, INSERT, UPDATE 
    ON application_settings, devices, positions, users, users_devices, user_settings
    TO traccar_user;

