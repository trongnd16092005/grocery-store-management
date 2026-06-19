BEGIN;

CREATE TABLE IF NOT EXISTS app_users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(120) NOT NULL,
    role            VARCHAR(20) NOT NULL DEFAULT 'CASHIER',
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_app_users_role CHECK (role IN ('ADMIN', 'CASHIER'))
);

CREATE TABLE IF NOT EXISTS categories (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(20) NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL UNIQUE,
    description     VARCHAR(500),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS suppliers (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(20) NOT NULL UNIQUE,
    name            VARCHAR(150) NOT NULL,
    phone           VARCHAR(20),
    email           VARCHAR(150),
    address         VARCHAR(500),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS products (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(20) NOT NULL UNIQUE,
    barcode         VARCHAR(50) UNIQUE,
    name            VARCHAR(180) NOT NULL,
    category_id     BIGINT NOT NULL REFERENCES categories(id),
    supplier_id     BIGINT REFERENCES suppliers(id) ON DELETE SET NULL,
    cost_price      NUMERIC(14,2) NOT NULL DEFAULT 0,
    selling_price   NUMERIC(14,2) NOT NULL,
    stock_quantity  INTEGER NOT NULL DEFAULT 0,
    minimum_stock   INTEGER NOT NULL DEFAULT 0,
    unit            VARCHAR(30) NOT NULL DEFAULT 'cái',
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_products_cost_price CHECK (cost_price >= 0),
    CONSTRAINT chk_products_selling_price CHECK (selling_price > 0),
    CONSTRAINT chk_products_stock CHECK (stock_quantity >= 0),
    CONSTRAINT chk_products_minimum_stock CHECK (minimum_stock >= 0)
);

CREATE TABLE IF NOT EXISTS customers (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(20) NOT NULL UNIQUE,
    full_name       VARCHAR(120) NOT NULL,
    phone           VARCHAR(20) NOT NULL UNIQUE,
    email           VARCHAR(150),
    gender          VARCHAR(20),
    address         VARCHAR(500),
    customer_type   VARCHAR(20) NOT NULL DEFAULT 'REGULAR',
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_customers_gender CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER')),
    CONSTRAINT chk_customers_type CHECK (customer_type IN ('REGULAR', 'LOYAL'))
);

CREATE TABLE IF NOT EXISTS purchase_orders (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(20) NOT NULL UNIQUE,
    supplier_id     BIGINT NOT NULL REFERENCES suppliers(id),
    created_by      BIGINT REFERENCES app_users(id) ON DELETE SET NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    total_amount    NUMERIC(14,2) NOT NULL DEFAULT 0,
    note            VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at    TIMESTAMPTZ,
    CONSTRAINT chk_purchase_orders_status CHECK (status IN ('DRAFT', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_purchase_orders_total CHECK (total_amount >= 0)
);

CREATE TABLE IF NOT EXISTS purchase_order_details (
    id                  BIGSERIAL PRIMARY KEY,
    purchase_order_id   BIGINT NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    product_id          BIGINT NOT NULL REFERENCES products(id),
    quantity            INTEGER NOT NULL,
    unit_cost           NUMERIC(14,2) NOT NULL,
    line_total          NUMERIC(14,2) GENERATED ALWAYS AS (quantity * unit_cost) STORED,
    CONSTRAINT uq_purchase_order_product UNIQUE (purchase_order_id, product_id),
    CONSTRAINT chk_purchase_details_quantity CHECK (quantity > 0),
    CONSTRAINT chk_purchase_details_cost CHECK (unit_cost >= 0)
);

CREATE TABLE IF NOT EXISTS invoices (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(20) NOT NULL UNIQUE,
    customer_id     BIGINT REFERENCES customers(id) ON DELETE SET NULL,
    cashier_id      BIGINT REFERENCES app_users(id) ON DELETE SET NULL,
    payment_method  VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PAID',
    subtotal        NUMERIC(14,2) NOT NULL,
    discount_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_amount    NUMERIC(14,2) NOT NULL,
    cash_received   NUMERIC(14,2),
    change_amount   NUMERIC(14,2),
    note            VARCHAR(500),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at    TIMESTAMPTZ,
    CONSTRAINT chk_invoices_payment CHECK (payment_method IN ('CASH', 'QR')),
    CONSTRAINT chk_invoices_status CHECK (status IN ('PENDING', 'PAID', 'CANCELLED')),
    CONSTRAINT chk_invoices_subtotal CHECK (subtotal >= 0),
    CONSTRAINT chk_invoices_discount CHECK (discount_amount >= 0),
    CONSTRAINT chk_invoices_total CHECK (total_amount >= 0),
    CONSTRAINT chk_invoices_cash CHECK (cash_received IS NULL OR cash_received >= 0),
    CONSTRAINT chk_invoices_change CHECK (change_amount IS NULL OR change_amount >= 0)
);

CREATE TABLE IF NOT EXISTS invoice_details (
    id              BIGSERIAL PRIMARY KEY,
    invoice_id      BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
    product_id      BIGINT REFERENCES products(id) ON DELETE SET NULL,
    product_code    VARCHAR(20) NOT NULL,
    product_name    VARCHAR(180) NOT NULL,
    unit_price      NUMERIC(14,2) NOT NULL,
    quantity        INTEGER NOT NULL,
    line_total      NUMERIC(14,2) GENERATED ALWAYS AS (quantity * unit_price) STORED,
    CONSTRAINT uq_invoice_product UNIQUE (invoice_id, product_code),
    CONSTRAINT chk_invoice_details_price CHECK (unit_price >= 0),
    CONSTRAINT chk_invoice_details_quantity CHECK (quantity > 0)
);

CREATE TABLE IF NOT EXISTS stock_transactions (
    id                  BIGSERIAL PRIMARY KEY,
    product_id          BIGINT NOT NULL REFERENCES products(id),
    transaction_type    VARCHAR(30) NOT NULL,
    quantity_change     INTEGER NOT NULL,
    stock_before        INTEGER NOT NULL,
    stock_after         INTEGER NOT NULL,
    reference_type      VARCHAR(30),
    reference_id        BIGINT,
    reason              VARCHAR(500),
    created_by          BIGINT REFERENCES app_users(id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_stock_transactions_type CHECK (
        transaction_type IN ('IMPORT', 'SALE', 'ADJUSTMENT', 'RETURN', 'CANCEL_SALE')
    ),
    CONSTRAINT chk_stock_transactions_change CHECK (quantity_change <> 0),
    CONSTRAINT chk_stock_transactions_before CHECK (stock_before >= 0),
    CONSTRAINT chk_stock_transactions_after CHECK (stock_after >= 0),
    CONSTRAINT chk_stock_transactions_balance CHECK (stock_after = stock_before + quantity_change)
);

CREATE INDEX IF NOT EXISTS idx_products_category ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_supplier ON products(supplier_id);
CREATE INDEX IF NOT EXISTS idx_products_name_lower ON products(LOWER(name));
CREATE INDEX IF NOT EXISTS idx_customers_name_lower ON customers(LOWER(full_name));
CREATE INDEX IF NOT EXISTS idx_invoices_created_at ON invoices(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_invoices_customer ON invoices(customer_id);
CREATE INDEX IF NOT EXISTS idx_stock_transactions_product_time
    ON stock_transactions(product_id, created_at DESC);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_app_users_updated_at ON app_users;
CREATE TRIGGER trg_app_users_updated_at
BEFORE UPDATE ON app_users
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_categories_updated_at ON categories;
CREATE TRIGGER trg_categories_updated_at
BEFORE UPDATE ON categories
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_suppliers_updated_at ON suppliers;
CREATE TRIGGER trg_suppliers_updated_at
BEFORE UPDATE ON suppliers
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_products_updated_at ON products;
CREATE TRIGGER trg_products_updated_at
BEFORE UPDATE ON products
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_customers_updated_at ON customers;
CREATE TRIGGER trg_customers_updated_at
BEFORE UPDATE ON customers
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE OR REPLACE VIEW low_stock_products AS
SELECT
    p.id,
    p.code,
    p.name,
    c.name AS category_name,
    p.stock_quantity,
    p.minimum_stock,
    CASE
        WHEN p.stock_quantity = 0 THEN 'OUT_OF_STOCK'
        WHEN p.stock_quantity <= p.minimum_stock THEN 'LOW_STOCK'
        ELSE 'IN_STOCK'
    END AS stock_status
FROM products p
JOIN categories c ON c.id = p.category_id
WHERE p.active = TRUE
  AND p.stock_quantity <= p.minimum_stock;

COMMIT;
