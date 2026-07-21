-- ============================================================================
-- Database creation
-- ============================================================================
-- Create one dedicated database per application.
-- This provides logical isolation between LinID, LemonLDAP and Superset.
-- ============================================================================

CREATE DATABASE linid;
CREATE DATABASE lemon;
CREATE DATABASE superset;
CREATE DATABASE datalake;

-- ============================================================================
-- Application roles
-- ============================================================================
-- Create dedicated PostgreSQL users for each application.
-- In production, passwords should be managed through secrets.
-- ============================================================================

CREATE ROLE linid_user WITH LOGIN PASSWORD 'password';
CREATE ROLE lemon_user WITH LOGIN PASSWORD 'password';
CREATE ROLE superset_user WITH LOGIN PASSWORD 'password';
CREATE ROLE datalake_user WITH LOGIN PASSWORD 'password';

-- ============================================================================
-- Database ownership
-- ============================================================================
-- Assign each database to its corresponding application user.
-- This allows each application to manage objects in its own database.
-- ============================================================================

ALTER DATABASE linid OWNER TO linid_user;
ALTER DATABASE lemon OWNER TO lemon_user;
ALTER DATABASE superset OWNER TO superset_user;
ALTER DATABASE datalake OWNER TO datalake_user;

-- ============================================================================
-- LinID database configuration
-- ============================================================================

\connect linid

-- Enable UUID generation functions used by LinID.
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Grant access to the public schema.
GRANT USAGE ON SCHEMA public TO linid_user;

-- Grant CRUD permissions on existing tables.
GRANT SELECT, INSERT, UPDATE, DELETE
    ON ALL TABLES IN SCHEMA public
    TO linid_user;

-- Grant access to existing sequences.
GRANT USAGE
    ON ALL SEQUENCES IN SCHEMA public
    TO linid_user;

-- Automatically grant permissions on future tables.
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE
    ON TABLES
    TO linid_user;

-- Automatically grant permissions on future sequences.
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE
    ON SEQUENCES
    TO linid_user;

-- ============================================================================
-- LemonLDAP database configuration
-- ============================================================================

\connect lemon

-- Enable UUID generation functions if required.
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

GRANT USAGE ON SCHEMA public TO lemon_user;

GRANT SELECT, INSERT, UPDATE, DELETE
    ON ALL TABLES IN SCHEMA public
    TO lemon_user;

GRANT USAGE
    ON ALL SEQUENCES IN SCHEMA public
    TO lemon_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE
    ON TABLES
    TO lemon_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE
    ON SEQUENCES
    TO lemon_user;

-- ============================================================================
-- Superset database configuration
-- ============================================================================
-- This database is used only for Superset metadata:
-- dashboards, charts, users, roles, datasets, etc.
-- Business data remains stored elsewhere.
-- ============================================================================

\connect superset

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

GRANT USAGE ON SCHEMA public TO superset_user;

GRANT SELECT, INSERT, UPDATE, DELETE
    ON ALL TABLES IN SCHEMA public
    TO superset_user;

GRANT USAGE
    ON ALL SEQUENCES IN SCHEMA public
    TO superset_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE
    ON TABLES
    TO superset_user;

ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT USAGE
    ON SEQUENCES
    TO superset_user;

-- ============================================================================
-- Datalake database configuration
-- ============================================================================
-- This database is dedicated to analytical and business data consumed by
-- Apache Superset.
--
-- Superset metadata (dashboards, charts, users, roles, datasets, etc.)
-- is stored separately in the "superset" database.
-- ============================================================================

\connect datalake

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

GRANT USAGE ON SCHEMA public TO datalake_user;

GRANT SELECT, INSERT, UPDATE, DELETE
    ON ALL TABLES IN SCHEMA public
    TO datalake_user;

GRANT USAGE, SELECT, UPDATE
    ON ALL SEQUENCES IN SCHEMA public
    TO datalake_user;

ALTER DEFAULT PRIVILEGES
    IN SCHEMA public
    GRANT SELECT, INSERT, UPDATE, DELETE
    ON TABLES
    TO datalake_user;

ALTER DEFAULT PRIVILEGES
    IN SCHEMA public
    GRANT USAGE, SELECT, UPDATE
    ON SEQUENCES
    TO datalake_user;
