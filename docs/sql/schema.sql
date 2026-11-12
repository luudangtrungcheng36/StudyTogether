-- Draft core schema for manual review and fresh-database setup.
-- Not a finalized baseline or Flyway migration. Finalize SQL before creating V1.
-- Run only on a fresh schema; do not run over retained data or existing tables.
-- The application must update updated_at on writes; defaults only cover inserts.
-- Application IDs may be supplied explicitly; defaults support manual inserts.

BEGIN;

CREATE TABLE public.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    display_name VARCHAR(255),
    avatar_url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    first_web_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.user_identities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    provider VARCHAR(32) NOT NULL DEFAULT 'DISCORD',
    provider_subject VARCHAR(255) NOT NULL,
    provider_display_name VARCHAR(255),
    provider_avatar_url TEXT,
    last_authenticated_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_identities_user
        FOREIGN KEY (user_id) REFERENCES public.users (id) ON DELETE RESTRICT,
    CONSTRAINT uq_user_identities_provider_subject
        UNIQUE (provider, provider_subject)
);

CREATE TABLE public.study_rooms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    discord_guild_id VARCHAR(20) NOT NULL,
    discord_channel_id VARCHAR(20) NOT NULL,
    display_name VARCHAR(255),
    tracking_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_study_rooms_guild_channel
        UNIQUE (discord_guild_id, discord_channel_id)
);

CREATE TABLE public.study_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    source VARCHAR(32) NOT NULL,
    identity_id UUID,
    study_room_id UUID,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    last_observed_at TIMESTAMPTZ NOT NULL,
    end_reason VARCHAR(40),
    needs_review BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_study_sessions_user
        FOREIGN KEY (user_id) REFERENCES public.users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_study_sessions_identity
        FOREIGN KEY (identity_id)
        REFERENCES public.user_identities (id) ON DELETE RESTRICT,
    CONSTRAINT fk_study_sessions_room
        FOREIGN KEY (study_room_id)
        REFERENCES public.study_rooms (id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX uq_study_sessions_open_discord_user
    ON public.study_sessions (user_id)
    WHERE source = 'DISCORD' AND ended_at IS NULL;

CREATE INDEX ix_study_sessions_user_started
    ON public.study_sessions (user_id, started_at DESC, id DESC);

CREATE INDEX ix_study_sessions_room
    ON public.study_sessions (study_room_id);

CREATE INDEX ix_study_sessions_identity
    ON public.study_sessions (identity_id);

CREATE TABLE public.study_session_segments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    study_session_id UUID NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    camera_on BOOLEAN NOT NULL DEFAULT FALSE,
    screen_share_on BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_study_session_segments_session
        FOREIGN KEY (study_session_id)
        REFERENCES public.study_sessions (id) ON DELETE RESTRICT,
    CONSTRAINT uq_study_session_segments_start
        UNIQUE (study_session_id, started_at)
);

CREATE UNIQUE INDEX uq_study_session_segments_open
    ON public.study_session_segments (study_session_id)
    WHERE ended_at IS NULL;

-- Service responsibilities: lock per user, create/close a session and segments
-- atomically, validate enabled rooms, reject bots, prevent historical overlaps,
-- require the session's identity to belong to its user, and ensure segments
-- record active visual study intervals (camera_on OR screen_share_on).
-- Explicit CHECK constraints are deferred; intended validation rules remain
-- documented in DATABASE.md for future service/constraint implementation.
-- Additional providers/sources require reviewed constraints and service rules.

COMMIT;
