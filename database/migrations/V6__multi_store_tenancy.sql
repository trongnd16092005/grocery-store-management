BEGIN;

CREATE TABLE IF NOT EXISTS stores (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(30) NOT NULL UNIQUE,
    name        VARCHAR(150) NOT NULL,
    phone       VARCHAR(20),
    address     VARCHAR(500),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO stores(code, name)
VALUES ('CUAHANGABC', 'Cửa hàng ABC')
ON CONFLICT (code) DO NOTHING;

CREATE OR REPLACE FUNCTION current_store_id()
RETURNS BIGINT AS $$
    SELECT NULLIF(current_setting('app.current_store_id', TRUE), '')::BIGINT;
$$ LANGUAGE SQL STABLE;

ALTER TABLE app_users ADD COLUMN IF NOT EXISTS store_id BIGINT REFERENCES stores(id);
ALTER TABLE categories ADD COLUMN IF NOT EXISTS store_id BIGINT REFERENCES stores(id);
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS store_id BIGINT REFERENCES stores(id);
ALTER TABLE products ADD COLUMN IF NOT EXISTS store_id BIGINT REFERENCES stores(id);
ALTER TABLE customers ADD COLUMN IF NOT EXISTS store_id BIGINT REFERENCES stores(id);
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS store_id BIGINT REFERENCES stores(id);
ALTER TABLE purchase_order_details ADD COLUMN IF NOT EXISTS store_id BIGINT REFERENCES stores(id);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS store_id BIGINT REFERENCES stores(id);
ALTER TABLE invoice_details ADD COLUMN IF NOT EXISTS store_id BIGINT REFERENCES stores(id);
ALTER TABLE stock_transactions ADD COLUMN IF NOT EXISTS store_id BIGINT REFERENCES stores(id);

DO $$
DECLARE default_store BIGINT;
BEGIN
    SELECT id INTO default_store FROM stores WHERE code='CUAHANGABC';
    UPDATE app_users SET store_id=default_store WHERE store_id IS NULL;
    UPDATE categories SET store_id=default_store WHERE store_id IS NULL;
    UPDATE suppliers SET store_id=default_store WHERE store_id IS NULL;
    UPDATE products SET store_id=default_store WHERE store_id IS NULL;
    UPDATE customers SET store_id=default_store WHERE store_id IS NULL;
    UPDATE purchase_orders SET store_id=default_store WHERE store_id IS NULL;
    UPDATE purchase_order_details SET store_id=default_store WHERE store_id IS NULL;
    UPDATE invoices SET store_id=default_store WHERE store_id IS NULL;
    UPDATE invoice_details SET store_id=default_store WHERE store_id IS NULL;
    UPDATE stock_transactions SET store_id=default_store WHERE store_id IS NULL;
END $$;

ALTER TABLE app_users ALTER COLUMN store_id SET NOT NULL;
ALTER TABLE categories ALTER COLUMN store_id SET NOT NULL;
ALTER TABLE suppliers ALTER COLUMN store_id SET NOT NULL;
ALTER TABLE products ALTER COLUMN store_id SET NOT NULL;
ALTER TABLE customers ALTER COLUMN store_id SET NOT NULL;
ALTER TABLE purchase_orders ALTER COLUMN store_id SET NOT NULL;
ALTER TABLE purchase_order_details ALTER COLUMN store_id SET NOT NULL;
ALTER TABLE invoices ALTER COLUMN store_id SET NOT NULL;
ALTER TABLE invoice_details ALTER COLUMN store_id SET NOT NULL;
ALTER TABLE stock_transactions ALTER COLUMN store_id SET NOT NULL;

ALTER TABLE app_users ALTER COLUMN store_id SET DEFAULT current_store_id();
ALTER TABLE categories ALTER COLUMN store_id SET DEFAULT current_store_id();
ALTER TABLE suppliers ALTER COLUMN store_id SET DEFAULT current_store_id();
ALTER TABLE products ALTER COLUMN store_id SET DEFAULT current_store_id();
ALTER TABLE customers ALTER COLUMN store_id SET DEFAULT current_store_id();
ALTER TABLE purchase_orders ALTER COLUMN store_id SET DEFAULT current_store_id();
ALTER TABLE purchase_order_details ALTER COLUMN store_id SET DEFAULT current_store_id();
ALTER TABLE invoices ALTER COLUMN store_id SET DEFAULT current_store_id();
ALTER TABLE invoice_details ALTER COLUMN store_id SET DEFAULT current_store_id();
ALTER TABLE stock_transactions ALTER COLUMN store_id SET DEFAULT current_store_id();

ALTER TABLE app_users DROP CONSTRAINT IF EXISTS app_users_username_key;
ALTER TABLE categories DROP CONSTRAINT IF EXISTS categories_code_key;
ALTER TABLE categories DROP CONSTRAINT IF EXISTS categories_name_key;
ALTER TABLE suppliers DROP CONSTRAINT IF EXISTS suppliers_code_key;
ALTER TABLE products DROP CONSTRAINT IF EXISTS products_code_key;
ALTER TABLE products DROP CONSTRAINT IF EXISTS products_barcode_key;
ALTER TABLE customers DROP CONSTRAINT IF EXISTS customers_code_key;
ALTER TABLE customers DROP CONSTRAINT IF EXISTS customers_phone_key;
ALTER TABLE purchase_orders DROP CONSTRAINT IF EXISTS purchase_orders_code_key;
ALTER TABLE invoices DROP CONSTRAINT IF EXISTS invoices_code_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_store_username ON app_users(store_id, LOWER(username));
CREATE UNIQUE INDEX IF NOT EXISTS uq_categories_store_code ON categories(store_id, code);
CREATE UNIQUE INDEX IF NOT EXISTS uq_categories_store_name ON categories(store_id, LOWER(name));
CREATE UNIQUE INDEX IF NOT EXISTS uq_suppliers_store_code ON suppliers(store_id, code);
CREATE UNIQUE INDEX IF NOT EXISTS uq_products_store_code ON products(store_id, code);
CREATE UNIQUE INDEX IF NOT EXISTS uq_products_store_barcode ON products(store_id, barcode) WHERE barcode IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_customers_store_code ON customers(store_id, code);
CREATE UNIQUE INDEX IF NOT EXISTS uq_customers_store_phone ON customers(store_id, phone);
CREATE UNIQUE INDEX IF NOT EXISTS uq_purchase_orders_store_code ON purchase_orders(store_id, code);
CREATE UNIQUE INDEX IF NOT EXISTS uq_invoices_store_code ON invoices(store_id, code);

DO $$
DECLARE table_name TEXT;
BEGIN
    FOREACH table_name IN ARRAY ARRAY[
        'app_users','categories','suppliers','products','customers',
        'purchase_orders','purchase_order_details','invoices',
        'invoice_details','stock_transactions'
    ] LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', table_name);
        EXECUTE format('ALTER TABLE %I FORCE ROW LEVEL SECURITY', table_name);
        EXECUTE format('DROP POLICY IF EXISTS tenant_isolation ON %I', table_name);
        EXECUTE format(
            'CREATE POLICY tenant_isolation ON %I USING (store_id=current_store_id()) WITH CHECK (store_id=current_store_id())',
            table_name
        );
    END LOOP;
END $$;

DROP VIEW IF EXISTS low_stock_products;
CREATE VIEW low_stock_products
WITH (security_invoker = true) AS
SELECT
    p.id, p.store_id, p.code, p.name, c.name AS category_name,
    p.stock_quantity, p.minimum_stock,
    CASE
        WHEN p.stock_quantity = 0 THEN 'OUT_OF_STOCK'
        WHEN p.stock_quantity <= p.minimum_stock THEN 'LOW_STOCK'
        ELSE 'IN_STOCK'
    END AS stock_status
FROM products p
JOIN categories c ON c.id = p.category_id
WHERE p.active = TRUE AND p.stock_quantity <= p.minimum_stock;

DROP TRIGGER IF EXISTS trg_stores_updated_at ON stores;
CREATE TRIGGER trg_stores_updated_at
BEFORE UPDATE ON stores
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMIT;
