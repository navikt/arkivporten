CREATE INDEX idx_document_link_id ON document (link_id);

CREATE INDEX idx_document_status ON document (status);

CREATE INDEX idx_document_orgnumber_created ON document (orgnumber, created);
