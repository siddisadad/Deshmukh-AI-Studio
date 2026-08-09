ALTER TABLE organizations
    ADD COLUMN slo_availability_target DOUBLE PRECISION NOT NULL DEFAULT 0.995,
    ADD COLUMN slo_latency_target DOUBLE PRECISION NOT NULL DEFAULT 0.95,
    ADD COLUMN slo_latency_threshold_seconds INT NOT NULL DEFAULT 2;
