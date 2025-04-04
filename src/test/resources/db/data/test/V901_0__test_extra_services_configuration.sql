-- Insert additional test service configurations
INSERT INTO service_configuration (service_name, label, url, list_order)
VALUES
    ('my-dynamic-service', 'Dynamic Service One', 'http://localhost:8090', 1000),
    ('my-alt-dynamic-service', 'Dynamic Service Two', 'http://localhost:8091', 1001);
