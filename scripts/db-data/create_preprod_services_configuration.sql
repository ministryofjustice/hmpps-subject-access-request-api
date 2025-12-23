-- Insert service configuration for the Preprod environment

INSERT INTO service_configuration (service_name, label, url, list_order, category)
VALUES
    ('G1', 'G1', 'G1', 1, 'PRISON'),
    ('G2', 'G2', 'G2', 2, 'PRISON'),
    ('G3', 'G3', 'G3', 3, 'PRISON'),
    ('hmpps-manage-adjudications-api', 'Adjudications', 'https://manage-adjudications-api-preprod.hmpps.service.justice.gov.uk', 4, 'PRISON'),
    ('keyworker-api', 'Allocate Keyworkers and Personal Officers', 'https://keyworker-api-preprod.prison.service.justice.gov.uk', 5, 'PRISON'),
    ('hmpps-book-secure-move-api', 'Book a secure move', 'https://hmpps-book-secure-move-api-preprod.apps.cloud-platform.service.justice.gov.uk', 6, 'PRISON'),
    ('hmpps-offender-categorisation-api', 'Categorisation tool', 'https://hmpps-offender-categorisation-api-preprod.hmpps.service.justice.gov.uk', 7, 'PRISON'),
    ('hmpps-complexity-of-need', 'Complexity of need', 'https://complexity-of-need-preprod.hmpps.service.justice.gov.uk', 8, 'PRISON'),
    ('create-and-vary-a-licence-api', 'Create and vary a licence', 'https://create-and-vary-a-licence-api-preprod.hmpps.service.justice.gov.uk', 9, 'PRISON'),
    ('hmpps-education-employment-api', 'Get someone ready to work', 'https://education-employment-api-preprod.hmpps.service.justice.gov.uk', 10, 'PRISON'),
    ('hmpps-health-and-medication-api', 'Health and medication', 'https://health-and-medication-api-preprod.hmpps.service.justice.gov.uk', 11, 'PRISON'),
    ('hmpps-hdc-api', 'Home detention curfew', 'https://hdc-api-preprod.hmpps.service.justice.gov.uk', 12, 'PRISON'),
    ('hmpps-incentives-api', 'Incentives', 'https://incentives-api-preprod.hmpps.service.justice.gov.uk', 13, 'PRISON'),
    ('launchpad-auth', 'Launchpad', 'https://launchpad-auth-preprod.hmpps.service.justice.gov.uk', 14, 'PRISON'),
    ('hmpps-education-and-work-plan-api', 'Learning and work progress', 'https://learningandworkprogress-api-preprod.hmpps.service.justice.gov.uk', 15, 'PRISON'),
    ('hmpps-activities-management-api', 'Manage activities and appointments', 'https://activities-api-preprod.prison.service.justice.gov.uk', 16, 'PRISON'),
    ('offender-management-allocation-manager', 'Manage Prison Offender Manager cases', 'https://preprod.moic.service.justice.gov.uk', 17, 'PRISON'),
    ('hmpps-managing-prisoner-apps-api', 'Managing prisoner applications', 'https://managing-prisoner-apps-api-preprod.hmpps.service.justice.gov.uk', 18, 'PRISON'),
    ('hmpps-jobs-board-api', 'Match jobs and manage applications', 'https://jobs-board-api-preprod.hmpps.service.justice.gov.uk', 19, 'PRISON'),
    ('hmpps-non-associations-api', 'Non-associations', 'https://non-associations-api-preprod.hmpps.service.justice.gov.uk', 20, 'PRISON'),
    ('hmpps-resettlement-passport-api', 'Prepare someone for release', 'https://resettlement-passport-api-preprod.hmpps.service.justice.gov.uk', 21, 'PRISON'),
    ('hmpps-restricted-patients-api', 'Restricted patients', 'https://restricted-patients-api-preprod.hmpps.service.justice.gov.uk', 22, 'PRISON'),
    ('offender-case-notes', 'Sensitive case notes', 'https://preprod.offender-case-notes.service.justice.gov.uk', 23, 'PRISON'),
    ('hmpps-support-additional-needs-api', 'Support for additional needs', 'https://support-for-additional-needs-api-preprod.hmpps.service.justice.gov.uk', 24, 'PRISON'),
    ('hmpps-uof-data-api', 'Use of force', 'https://hmpps-uof-data-api-preprod.hmpps.service.justice.gov.uk', 25, 'PRISON'),

    ('hmpps-accredited-programmes-api', 'Accredited programmes', 'https://accredited-programmes-api-preprod.hmpps.service.justice.gov.uk', 26, 'PROBATION'),
    ('hmpps-approved-premises-api', 'Community accommodation services', 'https://approved-premises-api-preprod.hmpps.service.justice.gov.uk', 27, 'PROBATION'),
    ('make-recall-decision-api', 'Consider a recall', 'https://make-recall-decision-api-preprod.hmpps.service.justice.gov.uk', 28, 'PROBATION'),
    ('court-case-service', 'Prepare a case for sentence', 'https://court-case-service-preprod.apps.live-1.cloud-platform.service.justice.gov.uk', 29, 'PROBATION'),
    ('hmpps-interventions-service', 'Refer and monitor and interventions', 'https://hmpps-interventions-service-preprod.apps.live-1.cloud-platform.service.justice.gov.uk', 30, 'PROBATION')
ON CONFLICT (service_name) DO UPDATE SET
    label = EXCLUDED.label,
    url = EXCLUDED.url,
    list_order = EXCLUDED.list_order,
    category = EXCLUDED.category;