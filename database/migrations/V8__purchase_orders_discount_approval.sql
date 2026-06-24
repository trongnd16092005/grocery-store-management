BEGIN;

ALTER TABLE purchase_orders
    ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS cancelled_by BIGINT;

ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS discount_approved_by BIGINT,
    ADD COLUMN IF NOT EXISTS discount_reason VARCHAR(500);

ALTER TABLE stock_transactions DROP CONSTRAINT IF EXISTS chk_stock_transactions_type;
ALTER TABLE stock_transactions ADD CONSTRAINT chk_stock_transactions_type CHECK (
    transaction_type IN (
        'IMPORT', 'CANCEL_IMPORT', 'SALE', 'ADJUSTMENT',
        'RETURN', 'CANCEL_SALE'
    )
);

ALTER TABLE purchase_orders DROP CONSTRAINT IF EXISTS fk_purchase_orders_cancelled_user_store;
ALTER TABLE purchase_orders ADD CONSTRAINT fk_purchase_orders_cancelled_user_store
    FOREIGN KEY (cancelled_by, store_id)
    REFERENCES app_users(id, store_id)
    ON DELETE SET NULL (cancelled_by);

ALTER TABLE invoices DROP CONSTRAINT IF EXISTS fk_invoices_discount_approver_store;
ALTER TABLE invoices ADD CONSTRAINT fk_invoices_discount_approver_store
    FOREIGN KEY (discount_approved_by, store_id)
    REFERENCES app_users(id, store_id)
    ON DELETE SET NULL (discount_approved_by);

CREATE INDEX IF NOT EXISTS idx_purchase_orders_created_at
    ON purchase_orders(store_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_purchase_orders_supplier
    ON purchase_orders(store_id, supplier_id);

COMMIT;
