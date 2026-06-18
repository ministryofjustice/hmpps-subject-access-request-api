-- Insert service configuration for the Preprod environment

INSERT INTO service_configuration (service_name, label, url, category)
VALUES
    ('G1', 'G1', 'G1','PRISON'),
    ('G2', 'G2', 'G2','PRISON'),
    ('G3', 'G3', 'G3','PRISON'),
    ('hmpps-manage-adjudications-api', 'Manage Adjudications', 'https://manage-adjudications-api-preprod.hmpps.service.justice.gov.uk','PRISON'),
    ('keyworker-api', 'Allocate Keyworkers and Personal Officers', 'https://keyworker-api-preprod.prison.service.justice.gov.uk','PRISON'),
    ('hmpps-book-secure-move-api', 'Book a secure move', 'https://hmpps-book-secure-move-api-preprod.apps.cloud-platform.service.justice.gov.uk','PRISON'),
    ('hmpps-offender-categorisation-api', 'Categorisation tool', 'https://hmpps-offender-categorisation-api-preprod.hmpps.service.justice.gov.uk','PRISON'),
    ('hmpps-complexity-of-need', 'Complexity of need', 'https://complexity-of-need-preprod.hmpps.service.justice.gov.uk','PRISON'),
    ('create-and-vary-a-licence-api', 'Create and vary a licence', 'https://create-and-vary-a-licence-api-preprod.hmpps.service.justice.gov.uk','PRISON'),
    ('hmpps-education-employment-api', 'Get someone ready to work', 'https://education-employment-api-preprod.hmpps.service.justice.gov.uk', 'PRISON'),
    ('hmpps-health-and-medication-api', 'Health and medication', 'https://health-and-medication-api-preprod.hmpps.service.justice.gov.uk','PRISON'),
    ('hmpps-hdc-api', 'Home detention curfew', 'https://hdc-api-preprod.hmpps.service.justice.gov.uk','PRISON'),
    ('hmpps-incentives-api', 'Incentives', 'https://incentives-api-preprod.hmpps.service.justice.gov.uk','PRISON'),
    ('launchpad-auth', 'Launchpad', 'https://launchpad-auth-preprod.hmpps.service.justice.gov.uk','PRISON'),
    ('hmpps-education-and-work-plan-api', 'Learning Work Progress (Personal Learning Plan)', 'https://learningandworkprogress-api-preprod.hmpps.service.justice.gov.uk','PRISON'),
    ('hmpps-activities-management-api', 'Manage activities and appointments', 'https://activities-api-preprod.prison.service.justice.gov.uk','PRISON'),
    ('offender-management-allocation-manager', 'Manage Prison Offender Manager cases', 'https://preprod.moic.service.justice.gov.uk','PRISON'),
    ('hmpps-managing-prisoner-apps-api', 'Managing prisoner applications', 'https://managing-prisoner-apps-api-preprod.hmpps.service.justice.gov.uk','PRISON'),
    ('hmpps-jobs-board-api', 'Match jobs and manage applications', 'https://jobs-board-api-preprod.hmpps.service.justice.gov.uk','PRISON'),
    ('hmpps-non-associations-api', 'Non-associations', 'https://non-associations-api-preprod.hmpps.service.justice.gov.uk','PRISON'),
    ('hmpps-resettlement-passport-api', 'Prepare someone for release', 'https://resettlement-passport-api-preprod.hmpps.service.justice.gov.uk','PRISON'),
    ('hmpps-restricted-patients-api', 'Restricted patients', 'https://restricted-patients-api-preprod.prison.service.justice.gov.uk','PRISON'),
    ('offender-case-notes', 'Sensitive case notes', 'https://preprod.offender-case-notes.service.justice.gov.uk','PRISON'),
    ('hmpps-support-additional-needs-api', 'Support for additional needs', 'https://support-for-additional-needs-api-preprod.hmpps.service.justice.gov.uk','PRISON'),
    ('hmpps-uof-data-api', 'Use of force', 'https://hmpps-uof-data-api-preprod.hmpps.service.justice.gov.uk','PRISON'),

    ('hmpps-accredited-programmes-api', 'Accredited programmes', 'https://accredited-programmes-api-preprod.hmpps.service.justice.gov.uk','PROBATION'),
    ('hmpps-approved-premises-api', 'Community accommodation services', 'https://approved-premises-api-preprod.hmpps.service.justice.gov.uk','PROBATION'),
    ('make-recall-decision-api', 'Consider a recall', 'https://make-recall-decision-api-preprod.hmpps.service.justice.gov.uk','PROBATION'),
    ('court-case-service', 'Prepare a case for sentence', 'https://court-case-service-preprod.apps.live-1.cloud-platform.service.justice.gov.uk','PROBATION'),
    ('hmpps-interventions-service', 'Refer and monitor and interventions', 'https://hmpps-interventions-service-preprod.apps.live-1.cloud-platform.service.justice.gov.uk','PROBATION')
ON CONFLICT (service_name) DO UPDATE SET
    label = EXCLUDED.label,
    url = EXCLUDED.url,
    category = EXCLUDED.category;