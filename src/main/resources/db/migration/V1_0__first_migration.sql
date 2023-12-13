CREATE TYPE status AS ENUM('Pending', 'Completed');

CREATE TABLE IF NOT EXISTS subject_access_request (
    id TEXT PRIMARY KEY,
    status status NOT NULL DEFAULT 'Pending',
    date_from DATE,
    date_to DATE NOT NULL DEFAULT CURRENT_DATE,
    sar_case_reference_number TEXT NOT NULL,
    services TEXT[] NOT NULL,
    nomis_id TEXT,
    ndelius_case_reference_id TEXT,
    hmpps_id TEXT,
    subject TEXT NOT NULL,
    requested_by TEXT NOT NULL,
    request_date_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    claim_date_time TIMESTAMP,
    object_url TEXT,
    presigned_url TEXT,
    claim_attempts SMALLINT DEFAULT 0
);

/* CHECK (nomisId IS NOT NULL OR ndeliusCaseReferenceId IS NOT NULL OR hmppsId IS NOT NULL), */
/* The check will happen on the application side */