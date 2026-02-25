ALTER table service_configuration
    ADD COLUMN suspended BOOLEAN DEFAULT FALSE;

UPDATE service_configuration
SET suspended = FALSE
WHERE suspended is NULL;

ALTER TABLE service_configuration
    ALTER COLUMN suspended SET NOT NULL;