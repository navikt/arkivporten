alter table dialogporten_dialog
    rename column dialog_id to dialogporten_uuid;

alter table dialogporten_dialog
    rename to dialog;

alter table document_content
    rename constraint fk_dialog_id to fk_document_id;
