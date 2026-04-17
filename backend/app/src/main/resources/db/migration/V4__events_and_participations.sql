-- Events and Participations. Participations track user location per event; the
-- last_position is a PostGIS geography point indexed with GIST for ST_DWithin
-- queries, and stale positions are filtered at query time (last_update > now - 5m).

CREATE TABLE events (
    event_id          UUID          PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_name        VARCHAR(200)  NOT NULL,
    venue             VARCHAR(300),
    expiration_code   TIMESTAMPTZ   NOT NULL,
    qr_code_hash      VARCHAR(128)  NOT NULL UNIQUE,
    qr_code_plaintext VARCHAR(64),
    centroid          GEOGRAPHY(POINT, 4326),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_events_expiration ON events (expiration_code);

CREATE TABLE participations (
    user_id              UUID                     NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    event_id             UUID                     NOT NULL REFERENCES events(event_id) ON DELETE CASCADE,
    selected_radius      SMALLINT                 NOT NULL DEFAULT 50 CHECK (selected_radius IN (20, 50, 100)),
    last_position        GEOGRAPHY(POINT, 4326),
    last_position_acc_m  REAL,
    last_update          TIMESTAMPTZ,
    joined_at            TIMESTAMPTZ              NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, event_id)
);

CREATE INDEX idx_participations_event ON participations (event_id);
CREATE INDEX idx_participations_position ON participations USING GIST (last_position);
CREATE INDEX idx_participations_lastupd ON participations (event_id, last_update DESC);
