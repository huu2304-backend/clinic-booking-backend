CREATE TABLE doctor_schedule
(
    id                BIGSERIAL PRIMARY KEY,
    doctor_profile_id BIGINT       NOT NULL,
    work_date         DATE         NOT NULL,
    start_time        TIME         NOT NULL,
    end_time          TIME         NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',

    CONSTRAINT fk_doctor_schedule_doctor_profile FOREIGN KEY (doctor_profile_id) REFERENCES doctor_profile (id),
    CONSTRAINT uq_doctor_schedule_slot UNIQUE (doctor_profile_id, work_date, start_time),
    CONSTRAINT chk_doctor_schedule_status CHECK (status IN ('AVAILABLE', 'BOOKED', 'CANCELLED'))
);
