CREATE TABLE IF NOT EXISTS subject_access_request_archive
(
    id                            UUID PRIMARY KEY,
    sar_id                        UUID                     NOT NULL,
    sar_status                    TEXT                     NOT NULL,
    sar_date_from                 DATE,
    sar_date_to                   DATE                     NOT NULL,
    sar_case_reference_number     TEXT                     NOT NULL,
    sar_nomis_id                  TEXT,
    sar_ndelius_case_reference_id TEXT,
    sar_requested_by              TEXT                     NOT NULL,
    sar_request_date_time         TIMESTAMP with time zone NOT NULL,
    sar_claim_date_time           TIMESTAMP with time zone,
    sar_claim_attempts            SMALLINT DEFAULT 0,
    sar_object_url                TEXT,
    sar_last_downloaded           TIMESTAMP with time zone,
    service_id                    UUID                     NOT NULL,
    service_name                  TEXT                     NOT NULL,
    service_label                 TEXT                     NOT NULL,
    service_url                   TEXT                     NOT NULL,
    service_enabled               boolean                  NOT NULL,
    service_template_migrated     boolean                  NOT NULL,
    service_category              TEXT                     NOT NULL,
    service_suspended             BOOLEAN                  NOT NULL,
    service_suspended_at          TIMESTAMP WITH TIME ZONE,
    request_render_status         TEXT                     NOT NULL,
    request_rendered_at           TIMESTAMP WITH TIME ZONE,
    template_version_id           UUID                     NOT NULL,
    template_version_status       TEXT                     NOT NULL,
    template_version_version      INTEGER                  NOT NULL,
    template_version_created_at   TIMESTAMP                NOT NULL,
    template_version_published_at TIMESTAMP,
    template_version_file_hash    TEXT                     NOT NULL
);

CREATE INDEX sar_id_index ON subject_access_request_archive (sar_id);
CREATE INDEX service_id_index ON subject_access_request_archive (service_id);
CREATE INDEX service_name_index ON subject_access_request_archive (service_name);

ALTER TABLE subject_access_request_archive ADD CONSTRAINT sar_id_service_id_unique UNIQUE(sar_id, service_id);