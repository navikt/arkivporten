create type DOCUMENT_STATUS as enum ('RECEIVED', 'PENDING', 'COMPLETED', 'ERROR');
create type DOCUMENT_TYPE as enum ('OPPFOLGINGSPLAN', 'DIALOGMOTE', 'UNDEFINED');

create table dialogporten_dialog
(
    id             BIGSERIAL                PRIMARY KEY,
    title          VARCHAR(255)             NOT NULL,
    summary        TEXT,
    fnr            VARCHAR(11)              NOT NULL,
    org_number     VARCHAR(9)               NOT NULL,
    dialog_id      UUID                     UNIQUE,
    created        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

create table document
(
    id              BIGSERIAL                PRIMARY KEY,
    document_id     UUID                     NOT NULL,
    type            DOCUMENT_TYPE            NOT NULL DEFAULT 'UNDEFINED',
    content         BYTEA                    NOT NULL,
    content_type    VARCHAR(100)             NOT NULL,
    title           VARCHAR(255)             NOT NULL,
    summary         TEXT,
    link_id         UUID                     UNIQUE NOT NULL,
    created         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    status          DOCUMENT_STATUS          NOT NULL DEFAULT 'RECEIVED',
    is_read         BOOLEAN                  NOT NULL DEFAULT false,
    dialog_id       BIGSERIAL                NOT NULL,
    transmission_id UUID,
    CONSTRAINT fk_dialog_id
        FOREIGN KEY (dialog_id)
        REFERENCES dialogporten_dialog(id)
);
