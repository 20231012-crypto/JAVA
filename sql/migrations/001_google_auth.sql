-- Adds Google Sign-In support for the CUSTOMER role (restricted to @eaut.edu.vn).
-- Non-destructive: existing rows default to auth_provider='LOCAL', google_sub=NULL,
-- and keep their existing password_hash untouched.

USE eaut_canteen;

ALTER TABLE users
  MODIFY password_hash VARCHAR(60) NULL,
  ADD COLUMN google_sub VARCHAR(255) NULL UNIQUE AFTER password_hash,
  ADD COLUMN auth_provider ENUM('LOCAL','GOOGLE') NOT NULL DEFAULT 'LOCAL' AFTER google_sub;
