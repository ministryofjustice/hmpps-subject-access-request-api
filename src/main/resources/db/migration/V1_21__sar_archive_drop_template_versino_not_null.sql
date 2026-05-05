/* Template Version columns will be null if Service hasn't been migrated yet */
ALTER TABLE subject_access_request_archive ALTER COLUMN template_version_id DROP NOT NULL;
ALTER TABLE subject_access_request_archive ALTER COLUMN template_version_status DROP NOT NULL;
ALTER TABLE subject_access_request_archive ALTER COLUMN template_version_version DROP NOT NULL;
ALTER TABLE subject_access_request_archive ALTER COLUMN template_version_created_at DROP NOT NULL;
ALTER TABLE subject_access_request_archive ALTER COLUMN template_version_published_at DROP NOT NULL;
ALTER TABLE subject_access_request_archive ALTER COLUMN template_version_file_hash DROP NOT NULL;