CREATE ROLE demo_dev_rw WITH LOGIN PASSWORD 'dev_database_passwd';
GRANT ALL PRIVILEGES ON DATABASE postgres TO demo_dev_rw;