-- Function to update timestamps automatically
CREATE OR REPLACE FUNCTION update_berths_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Function to update berth allocation timestamps
CREATE OR REPLACE FUNCTION update_berth_allocations_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Function to check berth allocation conflicts
CREATE OR REPLACE FUNCTION check_berth_allocation_conflict()
RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM berth_allocations
        WHERE berth_id = NEW.berth_id
        AND id != COALESCE(NEW.id, -1)
        AND status NOT IN ('COMPLETED', 'CANCELLED')
        AND (
            (NEW.start_time, NEW.end_time) OVERLAPS (start_time, COALESCE(end_time, start_time + INTERVAL '1 day'))
        )
    ) THEN
        RAISE EXCEPTION 'Berth allocation time conflict detected';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create berths table
CREATE TABLE berths (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    length FLOAT NOT NULL,
    depth FLOAT NOT NULL,
    max_vessel_size VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    port_id INTEGER NOT NULL,
    operational_status VARCHAR(50),
    last_maintenance_date TIMESTAMP,
    next_maintenance_date TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_berths_status CHECK (status IN ('AVAILABLE', 'OCCUPIED', 'UNDER_MAINTENANCE', 'RESERVED', 'OUT_OF_SERVICE')),
    CONSTRAINT chk_berths_length CHECK (length > 0),
    CONSTRAINT chk_berths_depth CHECK (depth > 0),
    CONSTRAINT fk_berths_port FOREIGN KEY (port_id) REFERENCES ports(id) ON DELETE RESTRICT
);

-- Create berth_allocations table
CREATE TABLE berth_allocations (
    id BIGSERIAL PRIMARY KEY,
    vessel_call_id BIGINT NOT NULL,
    berth_id INTEGER NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    allocation_priority INTEGER DEFAULT 0,
    conflict_resolution_notes TEXT,
    last_modified_by VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_berth_allocations_vessel_call FOREIGN KEY (vessel_call_id) REFERENCES vessel_calls(id) ON DELETE CASCADE,
    CONSTRAINT fk_berth_allocations_berth FOREIGN KEY (berth_id) REFERENCES berths(id) ON DELETE RESTRICT,
    CONSTRAINT chk_berth_allocations_status CHECK (status IN ('SCHEDULED', 'OCCUPIED', 'COMPLETED', 'CANCELLED', 'DELAYED', 'IN_PROGRESS')),
    CONSTRAINT chk_berth_allocations_time CHECK (end_time > start_time),
    CONSTRAINT chk_berth_allocations_priority CHECK (allocation_priority BETWEEN 0 AND 10)
);

-- Create indexes for berths table
CREATE INDEX idx_berths_port_id ON berths(port_id);
CREATE INDEX idx_berths_status ON berths(status);
CREATE INDEX idx_berths_port_status ON berths(port_id, status);
CREATE INDEX idx_berths_maintenance ON berths(next_maintenance_date);

-- Create indexes for berth_allocations table
CREATE INDEX idx_berth_allocations_vessel_call_id ON berth_allocations(vessel_call_id);
CREATE INDEX idx_berth_allocations_berth_id ON berth_allocations(berth_id);
CREATE INDEX idx_berth_allocations_time_range ON berth_allocations(berth_id, start_time, end_time);
CREATE INDEX idx_berth_allocations_status ON berth_allocations(status);
CREATE INDEX idx_berth_allocations_priority ON berth_allocations(allocation_priority);

-- Create triggers for automatic timestamp updates
CREATE TRIGGER berths_updated_at
    BEFORE UPDATE ON berths
    FOR EACH ROW
    EXECUTE FUNCTION update_berths_timestamp();

CREATE TRIGGER berth_allocations_updated_at
    BEFORE UPDATE ON berth_allocations
    FOR EACH ROW
    EXECUTE FUNCTION update_berth_allocations_timestamp();

-- Create trigger for conflict prevention
CREATE TRIGGER check_berth_allocation_conflicts
    BEFORE INSERT OR UPDATE ON berth_allocations
    FOR EACH ROW
    EXECUTE FUNCTION check_berth_allocation_conflict();