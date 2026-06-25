BEGIN;

ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS lifetime_loyalty_points INTEGER NOT NULL DEFAULT 0;

ALTER TABLE invoices
    ADD COLUMN IF NOT EXISTS points_redeemed INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS points_discount_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS points_earned INTEGER NOT NULL DEFAULT 0;

ALTER TABLE customers DROP CONSTRAINT IF EXISTS chk_customers_loyalty_points;
ALTER TABLE customers ADD CONSTRAINT chk_customers_loyalty_points
    CHECK (loyalty_points >= 0);

ALTER TABLE customers DROP CONSTRAINT IF EXISTS chk_customers_lifetime_points;
ALTER TABLE customers ADD CONSTRAINT chk_customers_lifetime_points
    CHECK (lifetime_loyalty_points >= 0);

ALTER TABLE invoices DROP CONSTRAINT IF EXISTS chk_invoices_points_redeemed;
ALTER TABLE invoices ADD CONSTRAINT chk_invoices_points_redeemed
    CHECK (points_redeemed >= 0);

ALTER TABLE invoices DROP CONSTRAINT IF EXISTS chk_invoices_points_discount;
ALTER TABLE invoices ADD CONSTRAINT chk_invoices_points_discount
    CHECK (points_discount_amount >= 0);

ALTER TABLE invoices DROP CONSTRAINT IF EXISTS chk_invoices_points_earned;
ALTER TABLE invoices ADD CONSTRAINT chk_invoices_points_earned
    CHECK (points_earned >= 0);

UPDATE invoices
SET points_earned = FLOOR(total_amount / 10000)::INTEGER
WHERE status = 'PAID'
  AND customer_id IS NOT NULL
  AND points_earned = 0;

WITH earned AS (
    SELECT
        c.id,
        COALESCE(SUM(i.points_earned), 0)::INTEGER points
    FROM customers c
    LEFT JOIN invoices i
      ON i.customer_id = c.id
     AND i.status = 'PAID'
    GROUP BY c.id
)
UPDATE customers c
SET loyalty_points = GREATEST(c.loyalty_points, earned.points),
    lifetime_loyalty_points = GREATEST(c.loyalty_points, earned.points),
    customer_type = CASE
        WHEN GREATEST(c.loyalty_points, earned.points) >= 300 THEN 'VIP'
        WHEN GREATEST(c.loyalty_points, earned.points) >= 50 THEN 'LOYAL'
        ELSE 'REGULAR'
    END
FROM earned
WHERE earned.id = c.id;

DO $$
DECLARE
    tenant RECORD;
BEGIN
    FOR tenant IN SELECT id FROM stores LOOP
        PERFORM set_config('app.current_store_id', tenant.id::TEXT, TRUE);

        UPDATE invoices
        SET points_earned = FLOOR(total_amount / 10000)::INTEGER
        WHERE status = 'PAID'
          AND customer_id IS NOT NULL
          AND points_earned = 0;

        WITH earned AS (
            SELECT
                c.id,
                COALESCE(SUM(i.points_earned), 0)::INTEGER points
            FROM customers c
            LEFT JOIN invoices i
              ON i.customer_id = c.id
             AND i.status = 'PAID'
            GROUP BY c.id
        )
        UPDATE customers c
        SET loyalty_points = GREATEST(c.loyalty_points, earned.points),
            lifetime_loyalty_points = GREATEST(c.lifetime_loyalty_points, earned.points),
            customer_type = CASE
                WHEN GREATEST(c.lifetime_loyalty_points, earned.points) >= 300 THEN 'VIP'
                WHEN GREATEST(c.lifetime_loyalty_points, earned.points) >= 50 THEN 'LOYAL'
                ELSE 'REGULAR'
            END
        FROM earned
        WHERE earned.id = c.id;

        UPDATE customers
        SET lifetime_loyalty_points = GREATEST(
            lifetime_loyalty_points,
            loyalty_points
        );
    END LOOP;
END $$;

COMMIT;
