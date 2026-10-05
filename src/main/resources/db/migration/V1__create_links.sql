CREATE TABLE links (
 id BIGINT PRIMARY KEY,
 short_code VARCHAR(32) NOT NULL,
 original_url VARCHAR(2048) NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
 expires_at TIMESTAMPTZ,
 click_count BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT uq_links_short_code UNIQUE (short_code)
);
CREATE INDEX idx_links_expires_at ON links (expires_at) WHERE expires_at IS NOT NULL;
