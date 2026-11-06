CREATE TABLE documents (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    org_unit      VARCHAR(100) NOT NULL,
    filename      VARCHAR(255) NOT NULL,
    content_type  VARCHAR(100) NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    uploaded_by   VARCHAR(100) NOT NULL,
    uploaded_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    processed_at  TIMESTAMPTZ,
    CONSTRAINT ck_documents_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);
CREATE INDEX idx_documents_org_unit ON documents (org_unit);

CREATE TABLE document_parents (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id  UUID NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    page_number  INT  NOT NULL,
    parent_index INT  NOT NULL,
    content      TEXT NOT NULL
);
CREATE INDEX idx_document_parents_document ON document_parents (document_id);

CREATE TABLE vector_store (
    id          UUID PRIMARY KEY,
    content     TEXT,
    metadata    JSONB,
    embedding   public.vector(768),
    content_tsv tsvector GENERATED ALWAYS AS (to_tsvector('simple', coalesce(content, ''))) STORED
);
CREATE INDEX idx_vector_store_embedding ON vector_store USING hnsw (embedding public.vector_cosine_ops);
CREATE INDEX idx_vector_store_content_tsv ON vector_store USING gin (content_tsv);
CREATE INDEX idx_vector_store_metadata ON vector_store USING gin (metadata jsonb_path_ops);
