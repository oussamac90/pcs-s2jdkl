-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Function to automatically update timestamps
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
EXCEPTION
    WHEN OTHERS THEN
        RAISE EXCEPTION 'Error in update_updated_at_column: %', SQLERRM;
END;
$$ LANGUAGE plpgsql;

-- Create vessels table
CREATE TABLE vessels (
    id BIGSERIAL PRIMARY KEY,
    imo_number VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(100),
    flag VARCHAR(100),
    length FLOAT,
    width FLOAT,
    max_draft FLOAT,
    owner VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for vessels table
CREATE INDEX idx_vessels_imo_number ON vessels(imo_number);
CREATE INDEX idx_vessels_name ON vessels(name);

-- Create berths table
CREATE TABLE berths (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    length FLOAT NOT NULL,
    depth FLOAT NOT NULL,
    max_vessel_size VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_berths_status CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'UNDER_MAINTENANCE')),
    CONSTRAINT chk_berths_length CHECK (length > 0),
    CONSTRAINT chk_berths_depth CHECK (depth > 0)
);

-- Create indexes for berths table
CREATE INDEX idx_berths_status ON berths(status);

-- Create vessel_calls table
CREATE TABLE vessel_calls (
    id BIGSERIAL PRIMARY KEY,
    vessel_id BIGINT NOT NULL,
    call_sign VARCHAR(255) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    eta TIMESTAMP NOT NULL,
    etd TIMESTAMP,
    ata TIMESTAMP,
    atd TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vessel_calls_vessel FOREIGN KEY (vessel_id) 
        REFERENCES vessels(id) ON DELETE RESTRICT,
    CONSTRAINT chk_vessel_calls_status 
        CHECK (status IN ('PLANNED', 'ARRIVED', 'AT_BERTH', 'DEPARTED', 'CANCELLED')),
    CONSTRAINT chk_vessel_calls_eta_etd CHECK (eta < etd),
    CONSTRAINT chk_vessel_calls_ata_atd CHECK (ata IS NULL OR ata <= atd)
);

-- Create indexes for vessel_calls table
CREATE INDEX idx_vessel_calls_vessel_id ON vessel_calls(vessel_id);
CREATE INDEX idx_vessel_calls_status ON vessel_calls(status);
CREATE INDEX idx_vessel_calls_eta_etd ON vessel_calls(eta, etd);
CREATE INDEX idx_vessel_calls_ata_atd ON vessel_calls(ata, atd);

-- Create berth_allocations table
CREATE TABLE berth_allocations (
    id BIGSERIAL PRIMARY KEY,
    vessel_call_id BIGINT NOT NULL,
    berth_id INTEGER NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_berth_allocations_vessel_call FOREIGN KEY (vessel_call_id) 
        REFERENCES vessel_calls(id) ON DELETE CASCADE,
    CONSTRAINT fk_berth_allocations_berth FOREIGN KEY (berth_id) 
        REFERENCES berths(id) ON DELETE RESTRICT,
    CONSTRAINT chk_berth_allocations_status 
        CHECK (status IN ('SCHEDULED', 'OCCUPIED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_berth_allocations_time CHECK (start_time < end_time),
    CONSTRAINT exclude_overlapping_allocations EXCLUDE USING gist 
        (berth_id WITH =, tsrange(start_time, end_time) WITH &&)
);

-- Create indexes for berth_allocations table
CREATE INDEX idx_berth_allocations_vessel_call_id ON berth_allocations(vessel_call_id);
CREATE INDEX idx_berth_allocations_berth_id ON berth_allocations(berth_id);
CREATE INDEX idx_berth_allocations_time_range ON berth_allocations(start_time, end_time);
CREATE INDEX idx_berth_allocations_status ON berth_allocations(status);

-- Create triggers for automatic timestamp updates
CREATE TRIGGER update_vessels_updated_at
    BEFORE UPDATE ON vessels
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_berths_updated_at
    BEFORE UPDATE ON berths
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_vessel_calls_updated_at
    BEFORE UPDATE ON vessel_calls
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_berth_allocations_updated_at
    BEFORE UPDATE ON berth_allocations
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Create comments for documentation
COMMENT ON TABLE vessels IS 'Stores vessel information with IMO number and physical characteristics';
COMMENT ON TABLE berths IS 'Stores berth information including physical dimensions and current status';
COMMENT ON TABLE vessel_calls IS 'Tracks vessel visits including estimated and actual arrival/departure times';
COMMENT ON TABLE berth_allocations IS 'Manages berth assignments with conflict prevention using time ranges';