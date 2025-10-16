create type DOCUMENT_STATUS as enum ('RECEIVED', 'PENDING', 'COMPLETED', 'ERROR');
create type DOCUMENT_TYPE as enum ('OPPFOLGINGSPLAN', 'DIALOGMOTE', 'UNDEFINED');

create table document
(
    id             BIGSERIAL PRIMARY KEY,
    document_id    UUID                     NOT NULL,
    type           DOCUMENT_TYPE            NOT NULL DEFAULT 'UNDEFINED',
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
