create table document
(
    id              BIGSERIAL PRIMARY KEY,
    document_id     UUID                     NOT NULL,
    type            VARCHAR(50)              NOT NULL,
    content         BYTEA                    NOT NULL,
    content_type    VARCHAR(100)             NOT NULL,
    orgnumber       VARCHAR(9)               NOT NULL,
    message_title   VARCHAR(255)             NOT NULL,
    message_summary TEXT                     NOT NULL,
    link_id         UUID                     NOT NULL,
    created         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    status          DOCUMENT_STATUS          NOT NULL DEFAULT 'RECEIVED',
    is_read         BOOLEAN                  NOT NULL DEFAULT false,
    message_id      UUID,
    CONSTRAINT uk_document_link_id UNIQUE (link_id),
    CONSTRAINT uk_document_message_id UNIQUE (message_id)
);
