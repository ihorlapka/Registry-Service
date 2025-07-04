CREATE TYPE user_roles AS ENUM ('ADMIN', 'USER');
CREATE TYPE device_statuses AS ENUM ('ONLINE', 'OFFLINE', 'ERROR', 'MAINTENANCE');
CREATE TYPE device_types AS ENUM ('THERMOSTAT', 'DOOR_SENSOR', 'SMART_LIGHT','ENERGY_METER', 'SMART_PLUG',
'TEMPERATURE_SENSOR', 'SOIL_MOISTURE_SENSOR');
CREATE TYPE device_manufacturers AS ENUM ('SAMSUNG', 'CISCO_SYSTEMS','HONEYWELL','BOSCH', 'SIEMENS', 'PHILIPS_HUE',
 'FITBIT', 'AMAZON', 'TEXAS_INSTRUMENTS', 'SCHNEIDER_ELECTRIC');

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) UNIQUE NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    email VARCHAR(255) UNIQUE NOT NULL,
    phone_number VARCHAR(20),
    address VARCHAR(255),
    password_hash VARCHAR(255) NOT NULL,
    user_role user_roles NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_login_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    serial_number VARCHAR(100) UNIQUE NOT NULL,
    manufacturer device_manufacturers,
    model VARCHAR(100),
    device_type device_types NOT NULL,
    location_description VARCHAR(255),
    latitude DECIMAL(9,6),
    longitude DECIMAL(9,6),
    owner_user_id UUID,
    status device_statuses NOT NULL DEFAULT 'OFFLINE',
    last_active_at TIMESTAMP WITH TIME ZONE,
    firmware_version VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    telemetry JSONB,
    CONSTRAINT fk_owner_user
        FOREIGN KEY (owner_user_id)
        REFERENCES users (id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);