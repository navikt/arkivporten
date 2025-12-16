alter table document
    alter column content drop not null;

create table document_content
(
    id              BIGSERIAL                PRIMARY KEY,
    content         BYTEA                    NOT NULL,
    CONSTRAINT fk_dialog_id
        FOREIGN KEY (id)
        REFERENCES document(id)
);
