-- Table to persist the template version health check results.
CREATE TABLE IF NOT EXISTS template_version_health_status
(
    id                       UUID PRIMARY KEY,
    service_configuration_id UUID                     NOT NULL,
    status                   TEXT                     NOT NULL,
    last_modified            TIMESTAMP with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    constraint fk_template_health_service_configuration FOREIGN KEY (service_configuration_id) REFERENCES service_configuration (id)
);

ALTER TABLE template_version_health_status
    ADD CONSTRAINT template_version_health_status_service_config_unique UNIQUE (service_configuration_id);

-- Use shedlock to ensure scheduled tasks only runs in single instance. When the service is deployed in an environment
-- there are will be multiple pods - we only want tasks to run once per interval not once per pod per interval.
CREATE TABLE shedlock
(
    name       VARCHAR(64)  NOT NULL PRIMARY KEY,
    lock_until TIMESTAMP(3) NULL,
    locked_at  TIMESTAMP(3) NULL,
    locked_by  VARCHAR(255) NULL
);
