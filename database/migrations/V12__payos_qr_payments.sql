BEGIN;

ALTER TABLE stock_transactions DROP CONSTRAINT IF EXISTS chk_stock_transactions_type;
ALTER TABLE stock_transactions ADD CONSTRAINT chk_stock_transactions_type CHECK (
    transaction_type IN (
        'IMPORT', 'SALE', 'ADJUSTMENT', 'RETURN', 'CANCEL_SALE',
        'PAYMENT_HOLD', 'PAYMENT_RELEASE'
    )
);

CREATE SEQUENCE IF NOT EXISTS payment_order_code_seq START WITH 100000;

CREATE TABLE IF NOT EXISTS payment_transactions (
    id                      BIGSERIAL PRIMARY KEY,
    store_id                BIGINT NOT NULL DEFAULT current_store_id()
                            REFERENCES stores(id),
    invoice_id              BIGINT NOT NULL,
    provider                VARCHAR(30) NOT NULL DEFAULT 'PAYOS',
    order_code              BIGINT NOT NULL,
    payment_link_id         VARCHAR(100),
    provider_reference      VARCHAR(120),
    amount                  NUMERIC(14,2) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    checkout_url            TEXT,
    qr_code                 TEXT,
    expires_at              TIMESTAMPTZ NOT NULL,
    paid_at                 TIMESTAMPTZ,
    cancelled_at            TIMESTAMPTZ,
    failure_reason          VARCHAR(500),
    raw_webhook             TEXT,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payment_invoice_store
        FOREIGN KEY (invoice_id, store_id) REFERENCES invoices(id, store_id),
    CONSTRAINT uq_payment_invoice UNIQUE (invoice_id),
    CONSTRAINT uq_payment_order_code UNIQUE (order_code),
    CONSTRAINT chk_payment_amount CHECK (amount > 0),
    CONSTRAINT chk_payment_status CHECK (
        status IN ('PENDING','PAID','CANCELLED','EXPIRED','FAILED','REVIEW')
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_provider_reference
    ON payment_transactions(provider, provider_reference)
    WHERE provider_reference IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_payment_status_expiry
    ON payment_transactions(status, expires_at);

ALTER TABLE payment_transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE payment_transactions FORCE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS tenant_isolation ON payment_transactions;
CREATE POLICY tenant_isolation ON payment_transactions
    USING (store_id=current_store_id())
    WITH CHECK (store_id=current_store_id());

CREATE TABLE IF NOT EXISTS payment_webhook_routes (
    order_code  BIGINT PRIMARY KEY,
    store_id    BIGINT NOT NULL REFERENCES stores(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DROP TRIGGER IF EXISTS trg_payment_transactions_updated_at ON payment_transactions;
CREATE TRIGGER trg_payment_transactions_updated_at
BEFORE UPDATE ON payment_transactions
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMIT;
