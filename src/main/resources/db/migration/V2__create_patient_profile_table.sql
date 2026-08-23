CREATE TABLE patient_profile
(
    id            BIGSERIAL PRIMARY KEY,
    account_id    BIGINT       NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    date_of_birth DATE,
    gender        VARCHAR(10),
    phone_number  VARCHAR(20),

    CONSTRAINT fk_patient_profile_account FOREIGN KEY (account_id) REFERENCES account (id),

    CONSTRAINT uq_patient_profile_account UNIQUE (account_id),
    CONSTRAINT chk_patient_profile_gender CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER')));