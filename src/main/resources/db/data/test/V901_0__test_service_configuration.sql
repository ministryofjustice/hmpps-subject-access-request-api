INSERT INTO service_configuration (id, service_name, label, url, list_order)
VALUES (random_uuid(), 'keyworker-api', 'Keyworker', 'https://keyworker-api-dev.prison.service.justice.gov.uk', 1),
       (random_uuid(), 'offender-case-notes', 'Sensitive Case Notes', 'https://dev.offender-case-notes.service.justice.gov.uk', 2),
       (random_uuid(), 'G1', 'G1', 'G1', 3),
       (random_uuid(), 'G2', 'G2', 'G2', 4),
       (random_uuid(), 'G3', 'G3', 'G3', 5);