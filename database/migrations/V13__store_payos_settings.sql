BEGIN;

ALTER TABLE stores
    ADD COLUMN IF NOT EXISTS payos_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS payos_client_id TEXT,
    ADD COLUMN IF NOT EXISTS payos_api_key TEXT,
    ADD COLUMN IF NOT EXISTS payos_checksum_key TEXT;

ALTER TABLE stores DROP CONSTRAINT IF EXISTS chk_stores_payos_complete;
ALTER TABLE stores ADD CONSTRAINT chk_stores_payos_complete CHECK (
    NOT payos_enabled OR (
        NULLIF(TRIM(payos_client_id), '') IS NOT NULL
        AND NULLIF(TRIM(payos_api_key), '') IS NOT NULL
        AND NULLIF(TRIM(payos_checksum_key), '') IS NOT NULL
    )
);

COMMIT;
