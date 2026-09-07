ALTER TABLE doctor_schedule
    ADD COLUMN locked_by_account_id BIGINT,
    ADD COLUMN lock_expires_at TIMESTAMP,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE doctor_schedule
    ADD CONSTRAINT fk_doctor_schedule_locked_by_account FOREIGN KEY (locked_by_account_id) REFERENCES account (id);

ALTER TABLE doctor_schedule
    DROP CONSTRAINT chk_doctor_schedule_status;

ALTER TABLE doctor_schedule
    ADD CONSTRAINT chk_doctor_schedule_status CHECK (status IN ('AVAILABLE', 'LOCKED', 'BOOKED', 'CANCELLED'));

-- Phục vụ job quét slot LOCKED hết hạn (CBS-52) chạy hiệu quả, không full table scan.
CREATE INDEX idx_doctor_schedule_status_lock_expires_at ON doctor_schedule (status, lock_expires_at);
