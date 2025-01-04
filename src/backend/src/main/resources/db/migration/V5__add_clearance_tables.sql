-- Create enum types for clearance management
CREATE TYPE clearance_type AS ENUM (
    'CUSTOMS',
    'IMMIGRATION',
    'PORT_AUTHORITY',
    'HEALTH',
    'SECURITY',
    'ENVIRONMENTAL',
    'DANGEROUS_GOODS'
);

CREATE TYPE clearance_status AS ENUM (
    'PENDING',
    'DOCUMENT_REVIEW',
    'INSPECTION_REQUIRED',
    'INSPECTION_IN_PROGRESS',
    'ADDITIONAL_INFO_REQUIRED',
    'IN_PROGRESS',
    'APPROVED',
    'REJECTED',
    'CANCELLED',
    'EXPIRED'
);

-- Function to validate clearance status transitions
CREATE OR REPLACE FUNCTION check_clearance_status_transition(old_status clearance_status, new_status clearance_status)
RETURNS boolean AS $$
BEGIN
    -- Define valid transitions
    RETURN CASE
        -- Initial state transitions
        WHEN old_status = 'PENDING' AND new_status IN ('DOCUMENT_REVIEW', 'INSPECTION_REQUIRED', 'IN_PROGRESS', 'CANCELLED') THEN true
        -- Document review transitions
        WHEN old_status = 'DOCUMENT_REVIEW' AND new_status IN ('INSPECTION_REQUIRED', 'ADDITIONAL_INFO_REQUIRED', 'APPROVED', 'REJECTED') THEN true
        -- Inspection transitions
        WHEN old_status = 'INSPECTION_REQUIRED' AND new_status IN ('INSPECTION_IN_PROGRESS', 'CANCELLED') THEN true
        WHEN old_status = 'INSPECTION_IN_PROGRESS' AND new_status IN ('APPROVED', 'ADDITIONAL_INFO_REQUIRED', 'REJECTED') THEN true
        -- Additional info transitions
        WHEN old_status = 'ADDITIONAL_INFO_REQUIRED' AND new_status IN ('DOCUMENT_REVIEW', 'INSPECTION_REQUIRED', 'IN_PROGRESS') THEN true
        -- In progress transitions
        WHEN old_status = 'IN_PROGRESS' AND new_status IN ('APPROVED', 'REJECTED', 'ADDITIONAL_INFO_REQUIRED') THEN true
        -- Final states
        WHEN old_status IN ('APPROVED', 'REJECTED', 'CANCELLED') AND new_status = 'EXPIRED' THEN true
        ELSE false
    END;
END;
$$ LANGUAGE plpgsql;

-- Create clearances table
CREATE TABLE clearances (
    id BIGSERIAL PRIMARY KEY,
    vessel_call_id BIGINT NOT NULL REFERENCES vessel_calls(id) ON DELETE RESTRICT,
    type clearance_type NOT NULL,
    status clearance_status NOT NULL DEFAULT 'PENDING',
    priority INTEGER NOT NULL DEFAULT 3,
    reference_number VARCHAR(255),
    submitted_by VARCHAR(255) NOT NULL,
    approved_by VARCHAR(255),
    inspector_id VARCHAR(255),
    inspection_notes TEXT,
    remarks TEXT,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at TIMESTAMP,
    last_checked_at TIMESTAMP,
    valid_from TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    valid_until TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT unique_vessel_call_clearance UNIQUE (vessel_call_id, type),
    CONSTRAINT valid_date_range CHECK (valid_until > valid_from),
    CONSTRAINT valid_approval_date CHECK (approved_at IS NULL OR approved_at >= submitted_at),
    CONSTRAINT valid_priority_range CHECK (priority BETWEEN 1 AND 5)
);

-- Create clearance documents table
CREATE TABLE clearance_documents (
    id BIGSERIAL PRIMARY KEY,
    clearance_id BIGINT NOT NULL REFERENCES clearances(id) ON DELETE CASCADE,
    document_type VARCHAR(100) NOT NULL,
    document_reference VARCHAR(255) NOT NULL,
    version INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    mime_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    hash VARCHAR(64) NOT NULL,
    uploaded_by VARCHAR(255) NOT NULL,
    verified_by VARCHAR(255),
    verified_at TIMESTAMP,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT valid_file_size CHECK (file_size > 0),
    CONSTRAINT valid_version CHECK (version > 0)
);

-- Create optimized indexes for clearances
CREATE INDEX idx_clearances_vessel_call_id ON clearances(vessel_call_id);
CREATE INDEX idx_clearances_type_status ON clearances(type, status);
CREATE INDEX idx_clearances_priority ON clearances(priority) WHERE status = 'PENDING';
CREATE INDEX idx_clearances_valid_dates ON clearances(valid_from, valid_until);
CREATE INDEX idx_clearances_submitted_at ON clearances(submitted_at);
CREATE INDEX idx_clearances_last_checked_at ON clearances(last_checked_at) 
    WHERE status NOT IN ('APPROVED', 'REJECTED', 'CANCELLED');

-- Create optimized indexes for clearance documents
CREATE INDEX idx_clearance_documents_clearance_id ON clearance_documents(clearance_id);
CREATE INDEX idx_clearance_documents_type_status ON clearance_documents(document_type, status);
CREATE INDEX idx_clearance_documents_hash ON clearance_documents(hash);
CREATE INDEX idx_clearance_documents_uploaded_at ON clearance_documents(uploaded_at);

-- Create trigger function for clearances updated_at
CREATE OR REPLACE FUNCTION update_clearances_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    -- Validate status transition if status is changing
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        IF NOT check_clearance_status_transition(OLD.status, NEW.status) THEN
            RAISE EXCEPTION 'Invalid clearance status transition from % to %', OLD.status, NEW.status;
        END IF;
    END IF;
    
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger function for clearance_documents updated_at
CREATE OR REPLACE FUNCTION update_clearance_documents_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        NEW.version = OLD.version + 1;
    END IF;
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create triggers
CREATE TRIGGER update_clearances_timestamp
    BEFORE UPDATE ON clearances
    FOR EACH ROW
    EXECUTE FUNCTION update_clearances_updated_at();

CREATE TRIGGER update_clearance_documents_timestamp
    BEFORE UPDATE ON clearance_documents
    FOR EACH ROW
    EXECUTE FUNCTION update_clearance_documents_updated_at();