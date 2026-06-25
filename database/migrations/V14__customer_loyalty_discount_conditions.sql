BEGIN;

ALTER TABLE customers DROP CONSTRAINT IF EXISTS chk_customers_type;
ALTER TABLE customers ADD CONSTRAINT chk_customers_type
    CHECK (customer_type IN ('REGULAR', 'LOYAL', 'VIP'));

ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS loyalty_points INTEGER NOT NULL DEFAULT 0;

ALTER TABLE discount_codes
    ADD COLUMN IF NOT EXISTS customer_type_scope VARCHAR(20) NOT NULL DEFAULT 'ALL',
    ADD COLUMN IF NOT EXISTS product_id BIGINT;

ALTER TABLE discount_codes DROP CONSTRAINT IF EXISTS chk_discount_customer_scope;
ALTER TABLE discount_codes ADD CONSTRAINT chk_discount_customer_scope
    CHECK (customer_type_scope IN ('ALL', 'REGULAR', 'LOYAL', 'VIP'));

ALTER TABLE discount_codes DROP CONSTRAINT IF EXISTS fk_discount_product_store;
ALTER TABLE discount_codes ADD CONSTRAINT fk_discount_product_store
    FOREIGN KEY (product_id, store_id)
    REFERENCES products(id, store_id)
    ON DELETE SET NULL (product_id);

CREATE INDEX IF NOT EXISTS idx_invoices_customer_created
    ON invoices(store_id, customer_id, created_at DESC)
    WHERE customer_id IS NOT NULL;

UPDATE customers c
SET loyalty_points = FLOOR(COALESCE(stats.total_spent, 0) / 10000)::INTEGER,
    customer_type = CASE
        WHEN COALESCE(stats.total_spent, 0) >= 3000000 THEN 'VIP'
        WHEN COALESCE(stats.total_spent, 0) >= 500000 THEN 'LOYAL'
        ELSE 'REGULAR'
    END
FROM (
    SELECT store_id, customer_id, COALESCE(SUM(total_amount), 0) AS total_spent
    FROM invoices
    WHERE status='PAID' AND customer_id IS NOT NULL
    GROUP BY store_id, customer_id
) stats
WHERE c.store_id = stats.store_id AND c.id = stats.customer_id;

COMMIT;
