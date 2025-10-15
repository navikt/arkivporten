create table document
(
    id             BIGSERIAL PRIMARY KEY,
    document_id    UUID                     NOT NULL,
    type           VARCHAR(50)              NOT NULL,
    content        BYTEA                    NOT NULL,
    content_type   VARCHAR(100)             NOT NULL,
    orgnumber      VARCHAR(9)               NOT NULL,
    dialog_title   VARCHAR(255)             NOT NULL,
    dialog_summary TEXT                     NOT NULL,
    link_id        UUID                     NOT NULL,
    created        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    status         DOCUMENT_STATUS          NOT NULL DEFAULT 'RECEIVED',
    is_read        BOOLEAN                  NOT NULL DEFAULT false,
    dialog_id      UUID,
    CONSTRAINT uq_document_link_id UNIQUE (link_id),
    CONSTRAINT uq_document_message_id UNIQUE (dialog_id)
);
