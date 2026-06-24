BEGIN;

CREATE TABLE IF NOT EXISTS discount_codes (
    id              BIGSERIAL PRIMARY KEY,
    store_id        BIGINT NOT NULL DEFAULT current_store_id()
                        REFERENCES stores(id),
    code            VARCHAR(40) NOT NULL,
    name            VARCHAR(150) NOT NULL,
    discount_type   VARCHAR(20) NOT NULL,
    discount_value  NUMERIC(14,2) NOT NULL,
    minimum_order   NUMERIC(14,2) NOT NULL DEFAULT 0,
    maximum_discount NUMERIC(14,2),
    starts_at       TIMESTAMPTZ,
    ends_at         TIMESTAMPTZ,
    usage_limit     INTEGER,
    used_count      INTEGER NOT NULL DEFAULT 0,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_discount_codes_type
        CHECK (discount_type IN ('PERCENT', 'AMOUNT')),
    CONSTRAINT chk_discount_codes_value
        CHECK (discount_value > 0),
    CONSTRAINT chk_discount_codes_percent
        CHECK (discount_type <> 'PERCENT' OR discount_value <= 100),
    CONSTRAINT chk_discount_codes_minimum
        CHECK (minimum_order >= 0),
    CONSTRAINT chk_discount_codes_maximum
        CHECK (maximum_discount IS NULL OR maximum_discount > 0),
    CONSTRAINT chk_discount_codes_usage
        CHECK (usage_limit IS NULL OR usage_limit > 0),
    CONSTRAINT chk_discount_codes_used
        CHECK (used_count >= 0),
    CONSTRAINT chk_discount_codes_dates
        CHECK (starts_at IS NULL OR ends_at IS NULL OR starts_at < ends_at)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_discount_codes_store_code
    ON discount_codes(store_id, UPPER(code));

ALTER TABLE discount_codes ENABLE ROW LEVEL SECURITY;
ALTER TABLE discount_codes FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON discount_codes;
CREATE POLICY tenant_isolation ON discount_codes
    USING (store_id=current_store_id())
    WITH CHECK (store_id=current_store_id());

DROP TRIGGER IF EXISTS trg_discount_codes_updated_at ON discount_codes;
CREATE TRIGGER trg_discount_codes_updated_at
BEFORE UPDATE ON discount_codes
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS discount_code_id BIGINT,
    ADD COLUMN IF NOT EXISTS discount_code VARCHAR(40);

ALTER TABLE discount_codes
    ADD CONSTRAINT uq_discount_codes_id_store UNIQUE (id, store_id);

ALTER TABLE invoices DROP CONSTRAINT IF EXISTS fk_invoices_discount_code_store;
ALTER TABLE invoices ADD CONSTRAINT fk_invoices_discount_code_store
    FOREIGN KEY (discount_code_id, store_id)
    REFERENCES discount_codes(id, store_id)
    ON DELETE SET NULL (discount_code_id);

COMMIT;
