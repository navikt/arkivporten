create table document
(
    id           BIGSERIAL PRIMARY KEY,
    document_id  UUID                     NOT NULL,
    orgnumber    VARCHAR(9)               NOT NULL,
    type         VARCHAR(50)              NOT NULL,
    asset_status DOCUMENT_STATUS          NOT NULL DEFAULT 'RECEIVED',
    content      BYTEA                    NOT NULL,
    isRead       BOOLEAN                  NOT NULL DEFAULT false,
    message_id   UUID,
    created      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
