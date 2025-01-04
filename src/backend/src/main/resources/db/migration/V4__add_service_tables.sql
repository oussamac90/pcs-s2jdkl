-- Create enum type for service categories
CREATE TYPE service_type AS ENUM (
    'PILOTAGE',    -- Vessel guidance services
    'TUGBOAT',     -- Vessel towing services
    'MOORING',     -- Vessel securing services
    'UNMOORING'    -- Vessel releasing services
);

-- Create enum type for service booking status lifecycle
CREATE TYPE service_status AS ENUM (
    'REQUESTED',    -- Initial booking state
    'CONFIRMED',    -- Service provider accepted
    'IN_PROGRESS',  -- Service currently being provided
    'COMPLETED',    -- Service successfully delivered
    'CANCELLED'     -- Service booking cancelled
);

-- Create main table for service bookings
CREATE TABLE service_bookings (
    id BIGSERIAL PRIMARY KEY,
    vessel_call_id BIGINT NOT NULL,
    service_type service_type NOT NULL,
    status service_status NOT NULL DEFAULT 'REQUESTED',
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    service_time TIMESTAMP NOT NULL,
    remarks TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (vessel_call_id) REFERENCES vessel_calls(id)
);

-- Create index for optimizing vessel call relationship queries
CREATE INDEX idx_service_bookings_vessel_call 
ON service_bookings(vessel_call_id);

-- Create index for optimizing schedule-based queries
CREATE INDEX idx_service_bookings_time 
ON service_bookings(service_time);

-- Add trigger to automatically update the updated_at timestamp
CREATE OR REPLACE FUNCTION update_service_bookings_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_service_bookings_timestamp
    BEFORE UPDATE ON service_bookings
    FOR EACH ROW
    EXECUTE FUNCTION update_service_bookings_timestamp();

-- Add comments for documentation
COMMENT ON TYPE service_type IS 'Valid categories of port services that can be booked';
COMMENT ON TYPE service_status IS 'Lifecycle states of a service booking';
COMMENT ON TABLE service_bookings IS 'Manages bookings of port services for vessel calls';
COMMENT ON COLUMN service_bookings.id IS 'Unique identifier for the service booking';
COMMENT ON COLUMN service_bookings.vessel_call_id IS 'Reference to the vessel call this service is booked for';
COMMENT ON COLUMN service_bookings.service_type IS 'Type of port service being booked';
COMMENT ON COLUMN service_bookings.status IS 'Current status of the service booking';
COMMENT ON COLUMN service_bookings.quantity IS 'Number of service units requested (e.g. number of tugboats)';
COMMENT ON COLUMN service_bookings.service_time IS 'Scheduled time for the service to be provided';
COMMENT ON COLUMN service_bookings.remarks IS 'Additional notes or special requirements for the service';
COMMENT ON COLUMN service_bookings.created_at IS 'Timestamp when the booking was created';
COMMENT ON COLUMN service_bookings.updated_at IS 'Timestamp when the booking was last updated';