CREATE TABLE IF NOT EXISTS SubjectAccessRequest (
    id VARCHAR(60) PRIMARY KEY,
    status VARCHAR NOT NULL,
    dateFrom DATE,
    dateTo DATE NOT NULL,
    sarCaseReferenceNumber VARCHAR NOT NULL,
    services VARCHAR NOT NULL, /* this will need to be array, need to find correct invalid syntax */
    nomisId VARCHAR,
    ndeliusCaseReferenceId VARCHAR,
    hmppsId VARCHAR,
    subject VARCHAR NOT NULL,
    requestedBy VARCHAR NOT NULL,
    requestDateTime TIMESTAMP NOT NULL,
    claimDateTime TIMESTAMP,
    objectURL VARCHAR,
    presignedURL VARCHAR,
    claimAttempts SMALLINT
);

/* CHECK (nomisId IS NOT NULL OR ndeliusCaseReferenceId IS NOT NULL OR hmppsId IS NOT NULL), */