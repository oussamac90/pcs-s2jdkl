-- Function to handle automatic timestamp updates
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.* IS DISTINCT FROM OLD.* THEN
        NEW.updated_at = CURRENT_TIMESTAMP AT TIME ZONE 'UTC';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Create vessels table
CREATE TABLE vessels (
    id BIGSERIAL PRIMARY KEY,
    imo_number VARCHAR(7) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50),
    flag VARCHAR(50),
    length NUMERIC(10,2),
    width NUMERIC(10,2),
    max_draft NUMERIC(10,2),
    owner VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create vessel_calls table
CREATE TABLE vessel_calls (
    id BIGSERIAL PRIMARY KEY,
    vessel_id BIGINT NOT NULL,
    call_sign VARCHAR(10) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    eta TIMESTAMP WITH TIME ZONE NOT NULL,
    etd TIMESTAMP WITH TIME ZONE,
    ata TIMESTAMP WITH TIME ZONE,
    atd TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (vessel_id) REFERENCES vessels(id) ON DELETE RESTRICT,
    CHECK (status IN ('PLANNED', 'ARRIVED', 'AT_BERTH', 'DEPARTED', 'CANCELLED')),
    CHECK (eta < etd),
    CHECK (ata IS NULL OR ata <= CURRENT_TIMESTAMP),
    CHECK (atd IS NULL OR (ata IS NOT NULL AND atd > ata))
);

-- Create indexes for vessels table
CREATE UNIQUE INDEX idx_vessels_imo_number ON vessels(imo_number);
CREATE INDEX idx_vessels_name ON vessels(name);
CREATE INDEX idx_vessels_type ON vessels(type) WHERE type IS NOT NULL;

-- Create indexes for vessel_calls table
CREATE INDEX idx_vessel_calls_vessel_id_status ON vessel_calls(vessel_id, status);
CREATE INDEX idx_vessel_calls_eta ON vessel_calls(eta) WHERE status = 'PLANNED';
CREATE INDEX idx_vessel_calls_status_dates ON vessel_calls(status, eta, etd);
CREATE INDEX idx_vessel_calls_call_sign_status ON vessel_calls(call_sign, status);

-- Create trigger for vessels table timestamp updates
CREATE TRIGGER vessels_updated_at_trigger
    BEFORE UPDATE ON vessels
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

-- Create trigger for vessel_calls table timestamp updates
CREATE TRIGGER vessel_calls_updated_at_trigger
    BEFORE UPDATE ON vessel_calls
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();