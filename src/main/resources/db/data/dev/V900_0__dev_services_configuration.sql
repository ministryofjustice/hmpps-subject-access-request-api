-- Insert dev env service configuration
INSERT INTO service_configuration (service_name, label, url, list_order)
VALUES ('keyworker-api', 'Keyworker', 'https://keyworker-api-dev.prison.service.justice.gov.uk', 1),
    ('offender-case-notes', 'Sensitive Case Notes', 'https://dev.offender-case-notes.service.justice.gov.uk', 2),
    ('court-case-service', 'Prepare a Case for Sentence', 'https://court-case-service-dev.apps.live-1.cloud-platform.service.justice.gov.uk', 3),
    ('hmpps-restricted-patients-api', 'Restricted Patients', 'https://restricted-patients-api-dev.hmpps.service.justice.gov.uk', 4),
    ('hmpps-accredited-programmes-api', 'Accredited Programmes', 'https://accredited-programmes-api-dev.hmpps.service.justice.gov.uk', 5),
    ('hmpps-complexity-of-need', 'Complexity Of Need', 'https://complexity-of-need-staging.hmpps.service.justice.gov.uk', 6),
    ('offender-management-allocation-manager', 'Manage Prison Offender Manager Cases', 'https://test.moic.service.justice.gov.uk', 7),
    ('hmpps-book-secure-move-api', 'Book a Secure Move', 'https://hmpps-book-secure-move-api-staging.apps.cloud-platform.service.justice.gov.uk', 8),
    ('hmpps-education-and-work-plan-api', 'Personal Learning Plan', 'https://learningandworkprogress-api-dev.hmpps.service.justice.gov.uk', 9),
    ('hmpps-non-associations-api', 'Non-associations', 'https://non-associations-api-dev.hmpps.service.justice.gov.uk', 10),
    ('hmpps-incentives-api', 'Incentives', 'https://incentives-api-dev.hmpps.service.justice.gov.uk', 11),
    ('hmpps-manage-adjudications-api', 'Manage Adjudications', 'https://manage-adjudications-api-dev.hmpps.service.justice.gov.uk', 12),
    ('hmpps-offender-categorisation-api', 'Categorisation Tool', 'https://hmpps-offender-categorisation-api-dev.hmpps.service.justice.gov.uk', 13),
    ('make-recall-decision-api', 'Consider a Recall', 'https://make-recall-decision-api-dev.hmpps.service.justice.gov.uk', 14),
    ('hmpps-hdc-api', 'Home Detention Curfew', 'https://hdc-api-dev.hmpps.service.justice.gov.uk', 15),
    ('create-and-vary-a-licence-api', 'Create and Vary a Licence', 'https://create-and-vary-a-licence-api-dev.hmpps.service.justice.gov.uk', 16),
    ('hmpps-uof-data-api', 'Use of Force', 'https://hmpps-uof-data-api-dev.hmpps.service.justice.gov.uk', 17),
    ('hmpps-activities-management-api', 'Manage Activities and Appointments', 'https://activities-api-dev.prison.service.justice.gov.uk', 18),
    ('hmpps-interventions-service', 'Refer and Monitor an Intervention', 'https://hmpps-interventions-service-dev.apps.live-1.cloud-platform.service.justice.gov.uk', 19),
    ('hmpps-resettlement-passport-api', 'Prepare Someone for Release', 'https://resettlement-passport-api-dev.hmpps.service.justice.gov.uk', 20),
    ('hmpps-approved-premises-api', 'Approved Premises', 'https://approved-premises-api-dev.hmpps.service.justice.gov.uk', 21),
    ('hmpps-education-employment-api', 'Education Employment', 'https://education-employment-api-dev.hmpps.service.justice.gov.uk', 22),
    ('launchpad-auth', 'Launchpad', 'https://launchpad-auth-dev.hmpps.service.justice.gov.uk', 24),
    ('G1', 'G1', 'G1', 25),
    ('G2', 'G2', 'G2', 26),
    ('G3', 'G3', 'G3', 27);
