# User Story

**As an** administrator of an organisation unit,
**I want to** create accounts for my team, upload our policy documents, and ask questions about them in plain language,
**so that** my team can find the most relevant passages, with their source document and page, without reading every file themselves.

## Tasks

### Task 1: Create a user account

- Only an `ADMIN` can create users, and no other role can. Anyone else who calls this endpoint is refused.
- The `ADMIN` calls `POST /api/auth/users` with a username (3–100 characters), a password (8–72 characters), a role (`ROLE_ADMIN`, `ROLE_COMPLIANCE_OFFICER` or `ROLE_USER`) and an organisation unit.
- A username that already exists is rejected.
- The password is stored as a hash. The response is `201 Created` with the new user's details, and the user can now sign in.

### Task 2: Sign in and get a token

- Log in through the gateway (`POST /api/auth/login`) with a username and password.
- Receive a signed JWT (Bearer) that carries the user's role and organisation unit.
- Every other request is authenticated with this token, and other services verify it with the public keys the auth service publishes (JWKS).

### Task 3: Upload a document for processing

- Send a file to `POST /api/ingestion/documents`. Only users with the `ADMIN` or `COMPLIANCE_OFFICER` role are allowed, and the request is refused before the file body is read if the role is missing.
- The file type is detected, and the document is stored under the user's organisation unit.
- The service replies `202 Accepted`. The document is parsed page by page, split into parent and child chunks, embedded and saved to the vector store.
- Poll `GET /api/ingestion/documents/{id}` to see its status (`PENDING`, `PROCESSING`, `COMPLETED` or `FAILED`, with an error message if it failed). A document from another organisation unit returns `404`.

### Task 4: Search the documents with a question

- Send the question to `POST /api/rag/search`, with an optional `topK` (default 5).
- The question may be rewritten into a few variations. Semantic (vector) search and keyword search run in parallel for each one, restricted to the caller's organisation unit.
- The rankings are fused into one list, with one result per parent chunk. A reranker then reorders them against the original question and drops weak matches. If the reranker is unavailable, the fused order is returned.
- Each result has the document id and name, the page, the matched snippet, the larger parent passage for context, the scores, and whether it was found semantically, by keyword, or both.
