-- Interests: text / uploaded article / linked article. Each carries an extracted
-- top-K keyword list plus a TF-IDF weight vector stored as JSONB {keyword: weight}.

CREATE TABLE interests (
    interest_id         UUID         PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID         NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    type                VARCHAR(30)  NOT NULL CHECK (type IN ('TEXT', 'ARTICLE_LOCAL', 'ARTICLE_LINK')),
    content             TEXT         NOT NULL,
    article_url         TEXT,
    article_object_key  TEXT,
    extracted_keywords  TEXT[]       NOT NULL DEFAULT '{}',
    keyword_vector      JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_interests_user ON interests (user_id);
CREATE INDEX idx_interests_keywords ON interests USING GIN (extracted_keywords);
