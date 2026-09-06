CREATE TABLE department
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,

    CONSTRAINT uq_department_name UNIQUE (name)
);