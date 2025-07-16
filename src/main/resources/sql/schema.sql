-- Drop the table if it already exists, to ensure a clean slate on each startup
DROP TABLE IF EXISTS payments;

CREATE TABLE payments (
    id SERIAL PRIMARY KEY,
    correlation_id VARCHAR(36) UNIQUE NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    processor_type VARCHAR(10) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);