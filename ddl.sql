-- Drop the table if it already exists, to ensure a clean slate on each startup
DROP TABLE IF EXISTS payments;

CREATE TABLE payments (
    id SERIAL PRIMARY KEY,
    correlation_id VARCHAR(36) UNIQUE NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    processor_type VARCHAR(10) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_processed_at ON payments (processed_at);
CREATE INDEX idx_payments_processor_type ON payments (processor_type);

-- CREATE INDEX idx_payments_processor_type_processed_at ON payments (processor_type, processed_at);