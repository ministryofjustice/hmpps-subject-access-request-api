-- Insert service configuration for the Preprod environment

INSERT INTO service_configuration (service_name, label, url, list_order)
VALUES
    ('G1', 'G1', 'G1', 1),
    ('G2', 'G2', 'G2', 2),
    ('G3', 'G3', 'G3', 3),
    ('hmpps-book-secure-move-api', 'Book a Secure Move', 'https://hmpps-book-secure-move-api-preprod.apps.cloud-platform.service.justice.gov.uk', 4),
    ('hmpps-offender-categorisation-api', 'Categorisation Tool', 'https://hmpps-offender-categorisation-api-preprod.hmpps.service.justice.gov.uk', 5),
    ('create-and-vary-a-licence-api', 'Create and Vary a Licence', 'https://create-and-vary-a-licence-api-preprod.hmpps.service.justice.gov.uk', 6),
    ('hmpps-hdc-api', 'Home Detention Curfew', 'https://hdc-api-preprod.hmpps.service.justice.gov.uk', 7),
    ('offender-management-allocation-manager', 'Manage Prison Offender Manager Cases', 'https://preprod.moic.service.justice.gov.uk', 8),
    ('hmpps-complexity-of-need', 'Complexity Of Need', 'https://complexity-of-need-preprod.hmpps.service.justice.gov.uk', 9),
    ('offender-case-notes', 'Sensitive Case Notes', 'https://preprod.offender-case-notes.service.justice.gov.uk', 10),
    ('hmpps-incentives-api', 'Incentives', 'https://incentives-api-preprod.hmpps.service.justice.gov.uk', 11),
    ('keyworker-api', 'Keyworker', 'https://keyworker-api-preprod.prison.service.justice.gov.uk', 12),
    ('hmpps-activities-management-api', 'Manage Activities and Appointments', 'https://activities-api-preprod.prison.service.justice.gov.uk', 13),
    ('hmpps-non-associations-api', 'Non-associations', 'https://non-associations-api-preprod.hmpps.service.justice.gov.uk', 14),
    ('hmpps-manage-adjudications-api', 'Manage Adjudications', 'https://manage-adjudications-api-preprod.hmpps.service.justice.gov.uk', 15),
    ('hmpps-uof-data-api', 'Use of Force', 'https://hmpps-uof-data-api-preprod.hmpps.service.justice.gov.uk', 16),
    ('hmpps-restricted-patients-api', 'Restricted Patients', 'https://restricted-patients-api-preprod.hmpps.service.justice.gov.uk', 17),
    ('launchpad-auth', 'Launchpad', 'https://launchpad-auth-preprod.hmpps.service.justice.gov.uk', 18),
    ('hmpps-education-and-work-plan-api', 'Personal Learning Plan', 'https://learningandworkprogress-api-preprod.hmpps.service.justice.gov.uk', 19),
    ('hmpps-education-employment-api', 'Work Readiness', 'https://education-employment-api-preprod.hmpps.service.justice.gov.uk', 20),
    ('hmpps-resettlement-passport-api', 'Prepare Someone for Release', 'https://resettlement-passport-api-preprod.hmpps.service.justice.gov.uk', 21),
    ('court-case-service', 'Prepare a Case for Sentence', 'https://court-case-service-preprod.apps.live-1.cloud-platform.service.justice.gov.uk', 22),
    ('hmpps-accredited-programmes-api', 'Accredited Programmes', 'https://accredited-programmes-api-preprod.hmpps.service.justice.gov.uk', 23),
    ('hmpps-interventions-service', 'Refer and Monitor an Intervention', 'https://hmpps-interventions-service-preprod.apps.live-1.cloud-platform.service.justice.gov.uk', 24),
    ('hmpps-approved-premises-api', 'Approved Premises', 'https://approved-premises-api-preprod.hmpps.service.justice.gov.uk', 25),
    ('make-recall-decision-api', 'Consider a Recall', 'https://make-recall-decision-api-preprod.hmpps.service.justice.gov.uk', 26),
    ('hmpps-health-and-medication-api', 'Health and Medication', 'https://health-and-medication-api-preprod.hmpps.service.justice.gov.uk', 27),
    ('hmpps-managing-prisoner-apps-api', 'Managing Prisoner Applications', 'https://managing-prisoner-apps-api-preprod.hmpps.service.justice.gov.uk', 28),
    ('hmpps-jobs-board-api', 'Match Jobs and Manage Applications', 'https://jobs-board-api-preprod.hmpps.service.justice.gov.uk', 29),
    ('hmpps-support-additional-needs-api', 'Support for Additional Needs', 'https://support-for-additional-needs-api-preprod.hmpps.service.justice.gov.uk', 30)
ON CONFLICT (service_name) DO UPDATE SET
    label = EXCLUDED.label,
    url = EXCLUDED.url,
    list_order = EXCLUDED.list_order;