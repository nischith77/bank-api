-- Create database if not exists
-- CREATE DATABASE IF NOT EXISTS vehicle_data;

-- Connect to the vehicle_data database
\c vehicle_data;

-- Create enum types for vehicle data
CREATE TYPE vehicle_status AS ENUM ('active', 'inactive', 'maintenance', 'retired');
CREATE TYPE fuel_type AS ENUM ('gasoline', 'diesel', 'electric', 'hybrid', 'lpg', 'cng');

-- Create vehicles table
CREATE TABLE IF NOT EXISTS vehicles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id VARCHAR(50) UNIQUE NOT NULL,
    make VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    year INTEGER NOT NULL CHECK (year >= 1900 AND year <= EXTRACT(YEAR FROM CURRENT_DATE) + 1),
    vin VARCHAR(17) UNIQUE,
    license_plate VARCHAR(20),
    fuel_type fuel_type NOT NULL,
    status vehicle_status DEFAULT 'active',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create vehicle_telemetry table for real-time data
CREATE TABLE IF NOT EXISTS vehicle_telemetry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    speed DECIMAL(5, 2), -- km/h
    heading DECIMAL(5, 2), -- degrees
    altitude DECIMAL(8, 2), -- meters
    fuel_level DECIMAL(5, 2), -- percentage
    engine_temperature DECIMAL(5, 2), -- celsius
    odometer DECIMAL(10, 2), -- kilometers
    battery_voltage DECIMAL(4, 2), -- volts
    engine_status BOOLEAN,
    ac_status BOOLEAN,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id) ON DELETE CASCADE
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_vehicle_telemetry_vehicle_id ON vehicle_telemetry(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_vehicle_telemetry_timestamp ON vehicle_telemetry(timestamp);
CREATE INDEX IF NOT EXISTS idx_vehicle_telemetry_vehicle_timestamp ON vehicle_telemetry(vehicle_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_vehicles_status ON vehicles(status);

-- Create vehicle_alerts table for storing alerts/notifications
CREATE TABLE IF NOT EXISTS vehicle_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vehicle_id VARCHAR(50) NOT NULL,
    alert_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('low', 'medium', 'high', 'critical')),
    message TEXT NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved BOOLEAN DEFAULT FALSE,
    resolved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_vehicle_alerts_vehicle_id ON vehicle_alerts(vehicle_id);
CREATE INDEX IF NOT EXISTS idx_vehicle_alerts_timestamp ON vehicle_alerts(timestamp);
CREATE INDEX IF NOT EXISTS idx_vehicle_alerts_resolved ON vehicle_alerts(resolved);

-- Create a function to update the updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger to automatically update updated_at
CREATE TRIGGER update_vehicles_updated_at BEFORE UPDATE ON vehicles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert some sample vehicles for testing
INSERT INTO vehicles (vehicle_id, make, model, year, vin, license_plate, fuel_type, status) VALUES
    ('VH001', 'Toyota', 'Camry', 2022, '1HGBH41JXMN109186', 'ABC-123', 'gasoline', 'active'),
    ('VH002', 'Tesla', 'Model 3', 2023, '5YJ3E1EA4KF123456', 'XYZ-789', 'electric', 'active'),
    ('VH003', 'Ford', 'F-150', 2021, '1FTFW1ET5DFC10312', 'DEF-456', 'gasoline', 'active'),
    ('VH004', 'BMW', 'X5', 2022, 'WBAFR9C5XDD123456', 'GHI-789', 'hybrid', 'maintenance')
ON CONFLICT (vehicle_id) DO NOTHING;