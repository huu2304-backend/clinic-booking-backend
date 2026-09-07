CREATE TABLE appointment
(
    id                  BIGSERIAL PRIMARY KEY,
    doctor_schedule_id  BIGINT      NOT NULL,
    patient_account_id  BIGINT      NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    created_at          TIMESTAMP   NOT NULL DEFAULT now(),
    cancelled_at        TIMESTAMP,

    CONSTRAINT uq_appointment_doctor_schedule UNIQUE (doctor_schedule_id),
    CONSTRAINT fk_appointment_doctor_schedule FOREIGN KEY (doctor_schedule_id) REFERENCES doctor_schedule (id),
    CONSTRAINT fk_appointment_patient_account FOREIGN KEY (patient_account_id) REFERENCES account (id),
    CONSTRAINT chk_appointment_status CHECK (status IN ('CONFIRMED', 'CANCELLED'))
);
