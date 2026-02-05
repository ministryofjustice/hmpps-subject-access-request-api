-- Insert service configuration for the Prod environment

INSERT INTO service_configuration (service_name, label, url, list_order, category)
VALUES
    ('G1', 'G1', 'G1', 1, 'PRISON'),
    ('G2', 'G2', 'G2', 2, 'PRISON'),
    ('G3', 'G3', 'G3', 3, 'PRISON'),
    ('hmpps-manage-adjudications-api', 'Adjudications', 'https://manage-adjudications-api.hmpps.service.justice.gov.uk', 4, 'PRISON'),
    ('keyworker-api', 'Allocate Keyworkers and Personal Officers', 'https://keyworker-api.prison.service.justice.gov.uk', 5, 'PRISON'),
    ('hmpps-book-secure-move-api', 'Book a secure move', 'https://hmpps-book-secure-move-api.apps.cloud-platform.service.justice.gov.uk', 6, 'PRISON'),
    ('hmpps-offender-categorisation-api', 'Categorisation tool', 'https://hmpps-offender-categorisation-api.hmpps.service.justice.gov.uk', 7, 'PRISON'),
    ('hmpps-complexity-of-need', 'Complexity of need', 'https://complexity-of-need.hmpps.service.justice.gov.uk', 8, 'PRISON'),
    ('create-and-vary-a-licence-api', 'Create and vary a licence', 'https://create-and-vary-a-licence-api.hmpps.service.justice.gov.uk', 9, 'PRISON'),
    ('hmpps-education-employment-api', 'Get someone ready to work', 'https://education-employment-api.hmpps.service.justice.gov.uk', 10, 'PRISON'),
    ('hmpps-health-and-medication-api', 'Health and medication', 'https://health-and-medication-api.hmpps.service.justice.gov.uk', 11, 'PRISON'),
    ('hmpps-hdc-api', 'Home detention curfew', 'https://hdc-api.hmpps.service.justice.gov.uk', 12, 'PRISON'),
    ('hmpps-incentives-api', 'Incentives', 'https://incentives-api.hmpps.service.justice.gov.uk', 13, 'PRISON'),
    ('launchpad-auth', 'Launchpad', 'https://launchpad-auth.hmpps.service.justice.gov.uk', 14, 'PRISON'),
    ('hmpps-education-and-work-plan-api', 'Learning and work progress', 'https://learningandworkprogress-api.hmpps.service.justice.gov.uk', 15, 'PRISON'),
    ('hmpps-activities-management-api', 'Manage activities and appointments', 'https://activities-api.prison.service.justice.gov.uk', 16, 'PRISON'),
    ('offender-management-allocation-manager', 'Manage Prison Offender Manager cases', 'https://moic.service.justice.gov.uk', 17, 'PRISON'),
    ('hmpps-managing-prisoner-apps-api', 'Managing prisoner applications', 'https://managing-prisoner-apps-api.hmpps.service.justice.gov.uk', 18, 'PRISON'),
    ('hmpps-jobs-board-api', 'Match jobs and manage applications', 'https://jobs-board-api.hmpps.service.justice.gov.uk', 19, 'PRISON'),
    ('hmpps-non-associations-api', 'Non-associations', 'https://non-associations-api.hmpps.service.justice.gov.uk', 20, 'PRISON'),
    ('hmpps-resettlement-passport-api', 'Prepare someone for release', 'https://resettlement-passport-api.hmpps.service.justice.gov.uk', 21, 'PRISON'),
    ('hmpps-restricted-patients-api', 'Restricted patients', 'https://restricted-patients-api.hmpps.service.justice.gov.uk', 22, 'PRISON'),
    ('offender-case-notes', 'Sensitive case notes', 'https://offender-case-notes.service.justice.gov.uk', 23, 'PRISON'),
    ('hmpps-support-additional-needs-api', 'Support for additional needs', 'https://support-for-additional-needs-api.hmpps.service.justice.gov.uk', 24, 'PRISON'),
    ('hmpps-uof-data-api', 'Use of force', 'https://hmpps-uof-data-api.hmpps.service.justice.gov.uk', 25, 'PRISON'),

    ('hmpps-accredited-programmes-api', 'Accredited programmes', 'https://accredited-programmes-api.hmpps.service.justice.gov.uk', 26, 'PROBATION'),
    ('hmpps-approved-premises-api', 'Community accommodation services', 'https://approved-premises-api.hmpps.service.justice.gov.uk', 27, 'PROBATION'),
    ('make-recall-decision-api', 'Consider a recall', 'https://make-recall-decision-api.hmpps.service.justice.gov.uk', 28, 'PROBATION'),
    ('court-case-service', 'Prepare a case for sentence', 'https://court-case-service.apps.live-1.cloud-platform.service.justice.gov.uk', 29, 'PROBATION'),
    ('hmpps-interventions-service', 'Refer and monitor and interventions', 'https://hmpps-interventions-service.apps.live-1.cloud-platform.service.justice.gov.uk', 30, 'PROBATION')
ON CONFLICT (service_name) DO UPDATE SET
    label = EXCLUDED.label,
    url = EXCLUDED.url,
    list_order = EXCLUDED.list_order,
    category = EXCLUDED.category;
