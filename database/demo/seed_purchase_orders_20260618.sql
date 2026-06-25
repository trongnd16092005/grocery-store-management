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
        SELECT 1 FROM purchase_orders
        WHERE note LIKE '%[DEMO-PO-20260618:%'
    ) THEN
        RAISE EXCEPTION
            'Dữ liệu phiếu nhập demo DEMO-PO-20260618 đã tồn tại.';
    END IF;
END $$;

CREATE TEMP TABLE demo_po_plan (
    demo_key VARCHAR(10) PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ NOT NULL,
    supplier_code VARCHAR(30) NOT NULL,
    username VARCHAR(50) NOT NULL
) ON COMMIT DROP;

INSERT INTO demo_po_plan VALUES
    ('P01', '2026-06-18 08:15:00+07', '2026-06-18 08:45:00+07', 'NCC001', 'admin'),
    ('P02', '2026-06-18 10:20:00+07', '2026-06-18 10:55:00+07', 'NCC001', 'qaz'),
    ('P03', '2026-06-18 13:10:00+07', '2026-06-18 13:40:00+07', 'NCC001', 'admin'),
    ('P04', '2026-06-18 15:30:00+07', '2026-06-18 16:05:00+07', 'NCC001', 'qaz'),
    ('P05', '2026-06-18 18:00:00+07', '2026-06-18 18:30:00+07', 'NCC001', 'admin');

CREATE TEMP TABLE demo_po_lines (
    demo_key VARCHAR(10) NOT NULL REFERENCES demo_po_plan(demo_key),
    product_code VARCHAR(30) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_cost NUMERIC(14,2) NOT NULL CHECK (unit_cost >= 0),
    PRIMARY KEY (demo_key, product_code)
) ON COMMIT DROP;

INSERT INTO demo_po_lines VALUES
    ('P01', 'SP001', 50, 7200),  ('P01', 'SP004', 50, 4200),
    ('P02', 'SP008', 40, 6800),  ('P02', 'SP009', 45, 6800),
    ('P03', 'SP010', 30, 7800),  ('P03', 'SP011', 25, 6200),
    ('P04', 'SP014', 20, 7000),  ('P04', 'SP015', 35, 3000),
    ('P05', 'SP017', 25, 2800),  ('P05', 'SP020', 10, 5000);

CREATE TEMP TABLE demo_po_totals AS
SELECT
    plan.demo_key,
    SUM(line.quantity * line.unit_cost)::NUMERIC(14,2) total_amount
FROM demo_po_plan plan
JOIN demo_po_lines line ON line.demo_key = plan.demo_key
GROUP BY plan.demo_key;

INSERT INTO purchase_orders(
    code,
    supplier_id,
    created_by,
    status,
    total_amount,
    note,
    created_at,
    completed_at,
    store_id
)
SELECT
    'PN' || LPAD(nextval('purchase_order_code_seq')::TEXT, 3, '0'),
    supplier.id,
    app_user.id,
    'COMPLETED',
    total.total_amount,
    'Dữ liệu minh họa phiếu nhập [DEMO-PO-20260618:' || plan.demo_key || ']',
    plan.created_at,
    plan.completed_at,
    current_store_id()
FROM demo_po_plan plan
JOIN demo_po_totals total ON total.demo_key = plan.demo_key
JOIN suppliers supplier ON supplier.code = plan.supplier_code AND supplier.active
JOIN app_users app_user ON LOWER(app_user.username) = LOWER(plan.username);

INSERT INTO purchase_order_details(
    purchase_order_id,
    product_id,
    quantity,
    unit_cost,
    store_id
)
SELECT
    purchase_order.id,
    product.id,
    line.quantity,
    line.unit_cost,
    current_store_id()
FROM demo_po_lines line
JOIN products product ON product.code = line.product_code AND product.active
JOIN purchase_orders purchase_order
  ON purchase_order.note = 'Dữ liệu minh họa phiếu nhập [DEMO-PO-20260618:'
      || line.demo_key || ']';

WITH first_stock AS (
    SELECT DISTINCT ON (product.id)
        product.id product_id,
        transaction.stock_before target_after
    FROM products product
    JOIN demo_po_lines line ON line.product_code = product.code
    LEFT JOIN stock_transactions transaction
      ON transaction.product_id = product.id
     AND transaction.created_at >= '2026-06-19 00:00:00+07'
    ORDER BY product.id, transaction.created_at, transaction.id
), fallback_stock AS (
    SELECT
        product.id product_id,
        COALESCE(first_stock.target_after, product.stock_quantity) target_after
    FROM products product
    JOIN demo_po_lines line ON line.product_code = product.code
    LEFT JOIN first_stock ON first_stock.product_id = product.id
    GROUP BY product.id, first_stock.target_after, product.stock_quantity
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
    product.id,
    'IMPORT',
    line.quantity,
    fallback.target_after - line.quantity,
    fallback.target_after,
    'PURCHASE_ORDER',
    purchase_order.id,
    'Nhập hàng - dữ liệu minh họa',
    purchase_order.created_by,
    purchase_order.completed_at,
    current_store_id()
FROM demo_po_lines line
JOIN products product ON product.code = line.product_code
JOIN fallback_stock fallback ON fallback.product_id = product.id
JOIN purchase_orders purchase_order
  ON purchase_order.note = 'Dữ liệu minh họa phiếu nhập [DEMO-PO-20260618:'
      || line.demo_key || ']';

COMMIT;

SELECT set_config(
    'app.current_store_id',
    (SELECT id::TEXT FROM stores WHERE code = 'CUAHANGABC'),
    FALSE
);

SELECT
    code,
    status,
    total_amount,
    created_at AT TIME ZONE 'Asia/Ho_Chi_Minh' local_created_at,
    completed_at AT TIME ZONE 'Asia/Ho_Chi_Minh' local_completed_at
FROM purchase_orders
WHERE note LIKE '%[DEMO-PO-20260618:%'
ORDER BY created_at;
