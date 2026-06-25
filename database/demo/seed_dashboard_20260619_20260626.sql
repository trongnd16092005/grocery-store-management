\set ON_ERROR_STOP on

BEGIN;

SET LOCAL TIME ZONE 'Asia/Ho_Chi_Minh';

SELECT set_config(
    'app.current_store_id',
    (SELECT id::TEXT FROM stores WHERE code = 'CUAHANGABC'),
    TRUE
);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM invoices
        WHERE note LIKE '%[DEMO-20260626:%'
    ) THEN
        RAISE EXCEPTION
            'Dữ liệu demo DEMO-20260626 đã tồn tại. Không chạy lại script.';
    END IF;
END $$;

CREATE TEMP TABLE demo_invoice_plan (
    demo_key          VARCHAR(10) PRIMARY KEY,
    sold_at           TIMESTAMPTZ NOT NULL,
    customer_code     VARCHAR(30) NOT NULL,
    cashier_username  VARCHAR(50) NOT NULL
) ON COMMIT DROP;

INSERT INTO demo_invoice_plan(
    demo_key, sold_at, customer_code, cashier_username
) VALUES
    ('D01', '2026-06-19 09:10:00+07', 'KH001', 'admin'),
    ('D02', '2026-06-19 17:35:00+07', 'KH002', 'qaz'),
    ('D03', '2026-06-20 10:20:00+07', 'KH003', 'admin'),
    ('D04', '2026-06-20 19:05:00+07', 'KH004', 'qaz'),
    ('D05', '2026-06-21 08:45:00+07', 'KH001', 'admin'),
    ('D06', '2026-06-21 16:30:00+07', 'KH005', 'qaz'),
    ('D07', '2026-06-22 11:15:00+07', 'KH002', 'admin'),
    ('D08', '2026-06-22 20:10:00+07', 'KH006', 'qaz'),
    ('D09', '2026-06-23 18:25:00+07', 'KH003', 'admin'),
    ('D10', '2026-06-24 09:40:00+07', 'KH001', 'qaz'),
    ('D11', '2026-06-24 19:15:00+07', 'KH008', 'admin'),
    ('D12', '2026-06-26 00:05:00+07', 'KH004', 'admin'),
    ('D13', '2026-06-26 00:15:00+07', 'KH005', 'qaz');

CREATE TEMP TABLE demo_line_plan (
    demo_key      VARCHAR(10) NOT NULL REFERENCES demo_invoice_plan(demo_key),
    product_code  VARCHAR(30) NOT NULL,
    quantity      INTEGER NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (demo_key, product_code)
) ON COMMIT DROP;

INSERT INTO demo_line_plan(demo_key, product_code, quantity) VALUES
    ('D01', 'SP001', 5), ('D01', 'SP004', 5), ('D01', 'SP008', 4),
    ('D02', 'SP009', 6), ('D02', 'SP010', 4),

    ('D03', 'SP001', 6), ('D03', 'SP011', 5), ('D03', 'SP014', 4),
    ('D04', 'SP004', 10), ('D04', 'SP015', 8), ('D04', 'SP017', 5),

    ('D05', 'SP008', 8), ('D05', 'SP009', 8), ('D05', 'SP010', 5),
    ('D06', 'SP001', 5), ('D06', 'SP004', 8), ('D06', 'SP020', 5),

    ('D07', 'SP001', 10), ('D07', 'SP008', 8), ('D07', 'SP010', 8),
    ('D08', 'SP009', 10), ('D08', 'SP011', 8), ('D08', 'SP014', 5),

    ('D09', 'SP004', 10), ('D09', 'SP015', 10),
    ('D09', 'SP017', 10), ('D09', 'SP020', 1),

    ('D10', 'SP001', 8), ('D10', 'SP008', 7), ('D10', 'SP010', 5),
    ('D11', 'SP009', 8), ('D11', 'SP011', 8), ('D11', 'SP014', 5),

    ('D12', 'SP001', 6), ('D12', 'SP004', 10), ('D12', 'SP008', 5),
    ('D13', 'SP009', 5), ('D13', 'SP010', 5), ('D13', 'SP015', 10);

DO $$
DECLARE
    missing_products INTEGER;
    missing_customers INTEGER;
    missing_cashiers INTEGER;
BEGIN
    SELECT COUNT(*) INTO missing_products
    FROM (
        SELECT DISTINCT line.product_code
        FROM demo_line_plan line
        LEFT JOIN products product
          ON product.code = line.product_code
         AND product.active
        WHERE product.id IS NULL
    ) missing;

    SELECT COUNT(*) INTO missing_customers
    FROM (
        SELECT DISTINCT plan.customer_code
        FROM demo_invoice_plan plan
        LEFT JOIN customers customer
          ON customer.code = plan.customer_code
         AND customer.active
        WHERE customer.id IS NULL
    ) missing;

    SELECT COUNT(*) INTO missing_cashiers
    FROM (
        SELECT DISTINCT plan.cashier_username
        FROM demo_invoice_plan plan
        LEFT JOIN app_users app_user
          ON LOWER(app_user.username) = LOWER(plan.cashier_username)
         AND app_user.active
        WHERE app_user.id IS NULL
    ) missing;

    IF missing_products > 0
        OR missing_customers > 0
        OR missing_cashiers > 0 THEN
        RAISE EXCEPTION
            'Thiếu dữ liệu nguồn: products=%, customers=%, cashiers=%',
            missing_products, missing_customers, missing_cashiers;
    END IF;
END $$;

CREATE TEMP TABLE demo_invoice_totals AS
SELECT
    plan.demo_key,
    plan.sold_at,
    plan.customer_code,
    plan.cashier_username,
    SUM(product.selling_price * line.quantity)::NUMERIC(14, 2) subtotal
FROM demo_invoice_plan plan
JOIN demo_line_plan line ON line.demo_key = plan.demo_key
JOIN products product ON product.code = line.product_code
GROUP BY
    plan.demo_key,
    plan.sold_at,
    plan.customer_code,
    plan.cashier_username;

DO $$
DECLARE
    invalid_days INTEGER;
BEGIN
    SELECT COUNT(*) INTO invalid_days
    FROM (
        SELECT
            sold_at::DATE sale_date,
            SUM(subtotal) daily_revenue
        FROM demo_invoice_totals
        GROUP BY sold_at::DATE
        HAVING SUM(subtotal) < 150000 OR SUM(subtotal) > 500000
    ) invalid;

    IF invalid_days > 0 THEN
        RAISE EXCEPTION
            'Kế hoạch demo có % ngày ngoài khoảng 150.000–500.000đ.',
            invalid_days;
    END IF;
END $$;

INSERT INTO invoices(
    code,
    customer_id,
    cashier_id,
    payment_method,
    status,
    subtotal,
    discount_amount,
    total_amount,
    cash_received,
    change_amount,
    note,
    created_at,
    store_id
)
SELECT
    'TMP-' || total.demo_key,
    customer.id,
    app_user.id,
    'CASH',
    'PAID',
    total.subtotal,
    0,
    total.subtotal,
    total.subtotal,
    0,
    'Dữ liệu minh họa dashboard [DEMO-20260626:' || total.demo_key || ']',
    total.sold_at,
    current_store_id()
FROM demo_invoice_totals total
JOIN customers customer ON customer.code = total.customer_code
JOIN app_users app_user
  ON LOWER(app_user.username) = LOWER(total.cashier_username);

INSERT INTO invoice_details(
    invoice_id,
    product_id,
    product_code,
    product_name,
    unit_price,
    quantity,
    store_id
)
SELECT
    invoice.id,
    product.id,
    product.code,
    product.name,
    product.selling_price,
    line.quantity,
    current_store_id()
FROM demo_line_plan line
JOIN products product ON product.code = line.product_code
JOIN invoices invoice
  ON invoice.note = 'Dữ liệu minh họa dashboard [DEMO-20260626:'
      || line.demo_key || ']';

-- Backfill transactions before the first real transaction on 25/06.
-- Current product stock is intentionally unchanged for these historical rows:
-- the inferred opening stock is increased so the historical chain ends exactly
-- at the stock_before value of the first existing transaction.
WITH historical_lines AS (
    SELECT
        invoice.id invoice_id,
        invoice.cashier_id,
        invoice.created_at,
        detail.product_id,
        detail.quantity,
        SUM(detail.quantity) OVER (
            PARTITION BY detail.product_id
        ) total_seed_quantity,
        COALESCE(
            (
                SELECT transaction.stock_before
                FROM stock_transactions transaction
                WHERE transaction.product_id = detail.product_id
                  AND transaction.created_at >= '2026-06-25 00:00:00+07'
                ORDER BY transaction.created_at, transaction.id
                LIMIT 1
            ),
            product.stock_quantity
        ) ending_stock,
        COALESCE(
            SUM(detail.quantity) OVER (
                PARTITION BY detail.product_id
                ORDER BY invoice.created_at, invoice.id, detail.id
                ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
            ),
            0
        ) quantity_before
    FROM invoices invoice
    JOIN invoice_details detail ON detail.invoice_id = invoice.id
    JOIN products product ON product.id = detail.product_id
    WHERE invoice.note LIKE '%[DEMO-20260626:%'
      AND invoice.created_at < '2026-06-25 00:00:00+07'
)
INSERT INTO stock_transactions(
    product_id,
    transaction_type,
    quantity_change,
    stock_before,
    stock_after,
    reference_type,
    reference_id,
    reason,
    created_by,
    created_at,
    store_id
)
SELECT
    product_id,
    'SALE',
    -quantity,
    ending_stock + total_seed_quantity - quantity_before,
    ending_stock + total_seed_quantity - quantity_before - quantity,
    'INVOICE',
    invoice_id,
    'Bán hàng - dữ liệu minh họa dashboard',
    cashier_id,
    created_at,
    current_store_id()
FROM historical_lines
ORDER BY created_at, invoice_id, product_id;

-- Today's demo sales happen after all existing data. They reduce current stock
-- and write a continuous stock history.
CREATE TEMP TABLE demo_today_stock AS
WITH today_lines AS (
    SELECT
        invoice.id invoice_id,
        invoice.cashier_id,
        invoice.created_at,
        detail.id detail_id,
        detail.product_id,
        detail.quantity,
        product.stock_quantity opening_stock,
        COALESCE(
            SUM(detail.quantity) OVER (
                PARTITION BY detail.product_id
                ORDER BY invoice.created_at, invoice.id, detail.id
                ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
            ),
            0
        ) quantity_before
    FROM invoices invoice
    JOIN invoice_details detail ON detail.invoice_id = invoice.id
    JOIN products product ON product.id = detail.product_id
    WHERE invoice.note LIKE '%[DEMO-20260626:%'
      AND invoice.created_at >= '2026-06-26 00:00:00+07'
)
SELECT
    invoice_id,
    cashier_id,
    created_at,
    product_id,
    quantity,
    opening_stock - quantity_before stock_before,
    opening_stock - quantity_before - quantity stock_after
FROM today_lines;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM demo_today_stock WHERE stock_after < 0
    ) THEN
        RAISE EXCEPTION
            'Không đủ tồn kho để tạo dữ liệu bán hàng ngày 26/06.';
    END IF;
END $$;

INSERT INTO stock_transactions(
    product_id,
    transaction_type,
    quantity_change,
    stock_before,
    stock_after,
    reference_type,
    reference_id,
    reason,
    created_by,
    created_at,
    store_id
)
SELECT
    product_id,
    'SALE',
    -quantity,
    stock_before,
    stock_after,
    'INVOICE',
    invoice_id,
    'Bán hàng - dữ liệu minh họa dashboard',
    cashier_id,
    created_at,
    current_store_id()
FROM demo_today_stock
ORDER BY created_at, invoice_id, product_id;

UPDATE products product
SET stock_quantity = product.stock_quantity - sold.total_quantity
FROM (
    SELECT product_id, SUM(quantity)::INTEGER total_quantity
    FROM demo_today_stock
    GROUP BY product_id
) sold
WHERE product.id = sold.product_id;

-- Recalculate loyalty from all paid invoices after linking demo orders.
WITH customer_spending AS (
    SELECT
        customer.id customer_id,
        COALESCE(SUM(invoice.total_amount), 0) total_spent
    FROM customers customer
    LEFT JOIN invoices invoice
      ON invoice.customer_id = customer.id
     AND invoice.status = 'PAID'
    GROUP BY customer.id
)
UPDATE customers customer
SET
    loyalty_points = FLOOR(spending.total_spent / 10000)::INTEGER,
    customer_type = CASE
        WHEN spending.total_spent >= 3000000 THEN 'VIP'
        WHEN spending.total_spent >= 500000 THEN 'LOYAL'
        ELSE 'REGULAR'
    END
FROM customer_spending spending
WHERE customer.id = spending.customer_id;

-- Move every invoice to a temporary namespace first to avoid unique conflicts,
-- then assign HD001... in chronological order.
UPDATE invoices
SET code = 'TMP-INV-' || id;

WITH ranked AS (
    SELECT
        id,
        ROW_NUMBER() OVER (ORDER BY created_at, id) sequence_number
    FROM invoices
)
UPDATE invoices invoice
SET code = 'HD' || LPAD(ranked.sequence_number::TEXT, 3, '0')
FROM ranked
WHERE invoice.id = ranked.id;

SELECT setval(
    'invoice_code_seq',
    COALESCE(
        (
            SELECT MAX(SUBSTRING(code FROM 3)::BIGINT)
            FROM invoices
            WHERE code ~ '^HD[0-9]+$'
        ),
        1
    ),
    TRUE
);

COMMIT;

-- Verification output
SELECT set_config(
    'app.current_store_id',
    (SELECT id::TEXT FROM stores WHERE code = 'CUAHANGABC'),
    FALSE
);

SELECT
    (created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::DATE sale_date,
    COUNT(*) FILTER (WHERE status = 'PAID') paid_invoices,
    COALESCE(
        SUM(total_amount) FILTER (WHERE status = 'PAID'),
        0
    ) paid_revenue
FROM invoices
WHERE (created_at AT TIME ZONE 'Asia/Ho_Chi_Minh')::DATE
      BETWEEN DATE '2026-06-19' AND DATE '2026-06-26'
GROUP BY sale_date
ORDER BY sale_date;

SELECT
    invoice.code,
    invoice.created_at AT TIME ZONE 'Asia/Ho_Chi_Minh' local_created_at,
    customer.code customer_code,
    customer.full_name customer_name,
    app_user.username cashier,
    invoice.total_amount,
    COUNT(detail.id) detail_count
FROM invoices invoice
LEFT JOIN customers customer ON customer.id = invoice.customer_id
LEFT JOIN app_users app_user ON app_user.id = invoice.cashier_id
LEFT JOIN invoice_details detail ON detail.invoice_id = invoice.id
GROUP BY
    invoice.id,
    customer.code,
    customer.full_name,
    app_user.username
ORDER BY invoice.created_at, invoice.id;
