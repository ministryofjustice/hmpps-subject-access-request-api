ALTER TABLE subject_access_request
ALTER COLUMN status TYPE text,
ALTER COLUMN status SET DEFAULT 'Pending',
ALTER COLUMN services TYPE text,
DROP COLUMN hmpps_id,
DROP COLUMN subject,
DROP COLUMN presigned_url;
DROP TYPE status_enum;