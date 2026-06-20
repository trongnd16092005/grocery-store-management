BEGIN;

CREATE SEQUENCE IF NOT EXISTS category_code_seq;
CREATE SEQUENCE IF NOT EXISTS product_code_seq;
CREATE SEQUENCE IF NOT EXISTS customer_code_seq;
CREATE SEQUENCE IF NOT EXISTS supplier_code_seq;
CREATE SEQUENCE IF NOT EXISTS purchase_order_code_seq;
CREATE SEQUENCE IF NOT EXISTS invoice_code_seq;

SELECT setval(
    'category_code_seq',
    COALESCE((SELECT MAX(SUBSTRING(code FROM 3)::BIGINT) FROM categories), 0) + 1,
    FALSE
);
SELECT setval(
    'product_code_seq',
    COALESCE((SELECT MAX(SUBSTRING(code FROM 3)::BIGINT) FROM products), 0) + 1,
    FALSE
);
SELECT setval(
    'customer_code_seq',
    COALESCE((SELECT MAX(SUBSTRING(code FROM 3)::BIGINT) FROM customers), 0) + 1,
    FALSE
);
SELECT setval(
    'supplier_code_seq',
    COALESCE((SELECT MAX(SUBSTRING(code FROM 4)::BIGINT) FROM suppliers), 0) + 1,
    FALSE
);
SELECT setval(
    'purchase_order_code_seq',
    COALESCE((SELECT MAX(SUBSTRING(code FROM 3)::BIGINT) FROM purchase_orders), 0) + 1,
    FALSE
);
SELECT setval(
    'invoice_code_seq',
    COALESCE((SELECT MAX(SUBSTRING(code FROM 3)::BIGINT) FROM invoices), 0) + 1,
    FALSE
);

COMMIT;
