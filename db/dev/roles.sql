CREATE ROLE demo_dev_rw WITH LOGIN PASSWORD 'dev_database_passwd';
GRANT ALL PRIVILEGES ON DATABASE postgres TO demo_dev_rw;
GRANT pg_read_all_data TO demo_dev_rw;
GRANT pg_write_all_data TO demo_dev_rw;