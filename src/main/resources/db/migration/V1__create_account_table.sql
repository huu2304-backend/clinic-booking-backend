CREATE TABLE account (
                         id BIGSERIAL PRIMARY KEY,
                         email VARCHAR(255) NOT NULL,
                         password_hash VARCHAR(255) NOT NULL,
                         role VARCHAR(20) NOT NULL,
                         status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                         created_at TIMESTAMP NOT NULL DEFAULT now(),
                         updated_at TIMESTAMP,

                         CONSTRAINT chk_account_role CHECK (role IN ('PATIENT', 'DOCTOR', 'ADMIN')),
                         CONSTRAINT chk_account_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED')),
                         CONSTRAINT uq_account_email UNIQUE (email)
);