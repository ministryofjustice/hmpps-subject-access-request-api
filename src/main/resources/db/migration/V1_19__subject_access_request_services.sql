ALTER table subject_access_request
    DROP COLUMN services;

CREATE TABLE IF NOT EXISTS request_service_detail (
    id UUID PRIMARY KEY,
    subject_access_request_id UUID NOT NULL,
    service_configuration_id UUID NOT NULL,
    render_status TEXT NOT NULL DEFAULT 'PENDING',
    template_version INTEGER DEFAULT NULL,
    rendered_at TIMESTAMP WITH TIME ZONE DEFAULT NULL,
    constraint fk_rs_subject_access_request foreign key (subject_access_request_id) REFERENCES subject_access_request (id),
    constraint fk_rs_service_configuration foreign key (service_configuration_id) REFERENCES service_configuration (id)
);
