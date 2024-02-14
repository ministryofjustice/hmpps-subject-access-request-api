ALTER TABLE subject_access_request DROP COLUMN id;
ALTER TABLE subject_access_request ADD COLUMN id UUID;
ALTER TABLE subject_access_request ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE subject_access_request ADD PRIMARY KEY (id);