CREATE TABLE doctor_profile
(
    id            BIGSERIAL PRIMARY KEY,
    account_id    BIGINT       NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    department_id BIGINT       NOT NULL,

    CONSTRAINT fk_doctor_profile_account FOREIGN KEY (account_id) REFERENCES account (id),
    CONSTRAINT fk_doctor_profile_department FOREIGN KEY (department_id) REFERENCES department (id),
    CONSTRAINT uq_doctor_profile_account UNIQUE (account_id)
);