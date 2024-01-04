ALTER TABLE subject_access_request
    ALTER COLUMN status TYPE TEXT,
    ALTER COLUMN status SET DEFAULT 'Pending',
    ALTER COLUMN services TYPE TEXT,
    DROP COLUMN hmpps_id,
    DROP COLUMN subject,
    DROP COLUMN presigned_url;
DROP TYPE status_enum;