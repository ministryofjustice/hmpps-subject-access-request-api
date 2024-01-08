ALTER TABLE subject_access_request ALTER COLUMN status TYPE text;
ALTER TABLE subject_access_request ALTER COLUMN status SET DEFAULT 'Pending';
ALTER TABLE subject_access_request ALTER COLUMN services TYPE text;
ALTER TABLE subject_access_request DROP COLUMN hmpps_id;
ALTER TABLE subject_access_request DROP COLUMN subject;
ALTER TABLE subject_access_request DROP COLUMN presigned_url;
DROP TYPE status_enum;