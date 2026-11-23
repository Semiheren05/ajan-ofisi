CREATE TABLE office_events (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_id  VARCHAR(100) NOT NULL,
    type        VARCHAR(30)  NOT NULL,
    agent_id    VARCHAR(100),
    agent_type  VARCHAR(100),
    tool_name   VARCHAR(100),
    detail      TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_office_events_type CHECK (type IN (
        'SESSION_STARTED', 'PROMPT', 'TOOL', 'AGENT_STARTED',
        'AGENT_STOPPED', 'WAITING', 'STOPPED', 'SESSION_ENDED'))
);

-- A session's history, in order
CREATE INDEX ix_office_events_session_id ON office_events (session_id, id);
-- The nightly cleanup and the "newest activity first" session list
CREATE INDEX ix_office_events_created_at ON office_events (created_at);
