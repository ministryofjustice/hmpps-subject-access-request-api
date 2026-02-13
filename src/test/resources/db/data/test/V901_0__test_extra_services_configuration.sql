-- Insert additional test service configurations
INSERT INTO service_configuration (service_name, label, url, category)
VALUES
    ('my-dynamic-service', 'Dynamic Service One', 'http://localhost:8090', 'PRISON'),
    ('my-alt-dynamic-service', 'Dynamic Service Two', 'http://localhost:8091', 'PRISON');
