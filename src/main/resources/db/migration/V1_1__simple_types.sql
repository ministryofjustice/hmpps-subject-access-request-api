ALTER TABLE subject_access_request ALTER COLUMN status TYPE text;
ALTER TABLE subject_access_request ALTER COLUMN status SET DEFAULT 'Pending';
ALTER TABLE subject_access_request ALTER COLUMN services TYPE text;
ALTER TABLE subject_access_request DROP COLUMN IF EXISTS hmpps_id;
ALTER TABLE subject_access_request DROP COLUMN IF EXISTS subject;
ALTER TABLE subject_access_request DROP COLUMN IF EXISTS presigned_url;
DROP TYPE IF EXISTS status_enum;