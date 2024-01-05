CREATE TYPE status_enum AS ENUM('Pending', 'Completed');

CREATE TABLE IF NOT EXISTS subject_access_request (
                                                      id SERIAL PRIMARY KEY,
                                                      status status_enum NOT NULL DEFAULT 'Pending',
                                                      date_from DATE,
                                                      date_to DATE NOT NULL DEFAULT CURRENT_DATE,
                                                      sar_case_reference_number TEXT NOT NULL,
                                                      services TEXT ARRAY NOT NULL,
                                                      nomis_id TEXT,
                                                      ndelius_case_reference_id TEXT,
                                                      hmpps_id TEXT,
                                                      subject TEXT NOT NULL,
                                                      requested_by TEXT NOT NULL,
                                                      request_date_time TIMESTAMP with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                      claim_date_time TIMESTAMP with time zone,
                                                      object_url TEXT,
                                                      presigned_url TEXT,
                                                      claim_attempts SMALLINT DEFAULT 0
);

CREATE INDEX status_index ON subject_access_request (status);
CREATE INDEX sar_case_reference_number_index ON subject_access_request (sar_case_reference_number);
CREATE INDEX subject_index ON subject_access_request (subject);
CREATE INDEX requested_by_index ON subject_access_request (requested_by);
CREATE INDEX claim_attempts_index ON subject_access_request (claim_attempts);

/* CHECK (nomisId IS NOT NULL OR ndeliusCaseReferenceId IS NOT NULL OR hmppsId IS NOT NULL), */
/* The check will happen on the application side */

ALTER TABLE subject_access_request ALTER COLUMN status TYPE text;
ALTER TABLE subject_access_request ALTER COLUMN status SET DEFAULT 'Pending';
ALTER TABLE subject_access_request ALTER COLUMN services TYPE text;
ALTER TABLE subject_access_request DROP COLUMN hmpps_id;
ALTER TABLE subject_access_request DROP COLUMN subject;
ALTER TABLE subject_access_request DROP COLUMN presigned_url;
DROP TYPE status_enum;