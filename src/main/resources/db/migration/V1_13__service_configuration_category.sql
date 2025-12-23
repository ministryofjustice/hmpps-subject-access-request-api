ALTER TABLE service_configuration
    ADD COLUMN category varchar(50);

UPDATE service_configuration
    SET category = 'PRISON';

ALTER TABLE service_configuration
    ALTER COLUMN category SET NOT NULL;

