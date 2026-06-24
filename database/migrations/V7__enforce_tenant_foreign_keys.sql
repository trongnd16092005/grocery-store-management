BEGIN;

ALTER TABLE app_users ADD CONSTRAINT uq_app_users_id_store UNIQUE (id, store_id);
ALTER TABLE categories ADD CONSTRAINT uq_categories_id_store UNIQUE (id, store_id);
ALTER TABLE suppliers ADD CONSTRAINT uq_suppliers_id_store UNIQUE (id, store_id);
ALTER TABLE products ADD CONSTRAINT uq_products_id_store UNIQUE (id, store_id);
ALTER TABLE customers ADD CONSTRAINT uq_customers_id_store UNIQUE (id, store_id);
ALTER TABLE purchase_orders ADD CONSTRAINT uq_purchase_orders_id_store UNIQUE (id, store_id);
ALTER TABLE invoices ADD CONSTRAINT uq_invoices_id_store UNIQUE (id, store_id);

ALTER TABLE products DROP CONSTRAINT products_category_id_fkey;
ALTER TABLE products ADD CONSTRAINT fk_products_category_store
    FOREIGN KEY (category_id, store_id) REFERENCES categories(id, store_id);
ALTER TABLE products DROP CONSTRAINT products_supplier_id_fkey;
ALTER TABLE products ADD CONSTRAINT fk_products_supplier_store
    FOREIGN KEY (supplier_id, store_id) REFERENCES suppliers(id, store_id);

ALTER TABLE purchase_orders DROP CONSTRAINT purchase_orders_supplier_id_fkey;
ALTER TABLE purchase_orders ADD CONSTRAINT fk_purchase_orders_supplier_store
    FOREIGN KEY (supplier_id, store_id) REFERENCES suppliers(id, store_id);
ALTER TABLE purchase_orders DROP CONSTRAINT purchase_orders_created_by_fkey;
ALTER TABLE purchase_orders ADD CONSTRAINT fk_purchase_orders_user_store
    FOREIGN KEY (created_by, store_id) REFERENCES app_users(id, store_id) ON DELETE SET NULL (created_by);

ALTER TABLE purchase_order_details DROP CONSTRAINT purchase_order_details_purchase_order_id_fkey;
ALTER TABLE purchase_order_details ADD CONSTRAINT fk_purchase_details_order_store
    FOREIGN KEY (purchase_order_id, store_id) REFERENCES purchase_orders(id, store_id) ON DELETE CASCADE;
ALTER TABLE purchase_order_details DROP CONSTRAINT purchase_order_details_product_id_fkey;
ALTER TABLE purchase_order_details ADD CONSTRAINT fk_purchase_details_product_store
    FOREIGN KEY (product_id, store_id) REFERENCES products(id, store_id);

ALTER TABLE invoices DROP CONSTRAINT invoices_customer_id_fkey;
ALTER TABLE invoices ADD CONSTRAINT fk_invoices_customer_store
    FOREIGN KEY (customer_id, store_id) REFERENCES customers(id, store_id) ON DELETE SET NULL (customer_id);
ALTER TABLE invoices DROP CONSTRAINT invoices_cashier_id_fkey;
ALTER TABLE invoices ADD CONSTRAINT fk_invoices_cashier_store
    FOREIGN KEY (cashier_id, store_id) REFERENCES app_users(id, store_id) ON DELETE SET NULL (cashier_id);

ALTER TABLE invoice_details DROP CONSTRAINT invoice_details_invoice_id_fkey;
ALTER TABLE invoice_details ADD CONSTRAINT fk_invoice_details_invoice_store
    FOREIGN KEY (invoice_id, store_id) REFERENCES invoices(id, store_id) ON DELETE CASCADE;
ALTER TABLE invoice_details DROP CONSTRAINT invoice_details_product_id_fkey;
ALTER TABLE invoice_details ADD CONSTRAINT fk_invoice_details_product_store
    FOREIGN KEY (product_id, store_id) REFERENCES products(id, store_id) ON DELETE SET NULL (product_id);

ALTER TABLE stock_transactions DROP CONSTRAINT stock_transactions_product_id_fkey;
ALTER TABLE stock_transactions ADD CONSTRAINT fk_stock_product_store
    FOREIGN KEY (product_id, store_id) REFERENCES products(id, store_id);
ALTER TABLE stock_transactions DROP CONSTRAINT stock_transactions_created_by_fkey;
ALTER TABLE stock_transactions ADD CONSTRAINT fk_stock_user_store
    FOREIGN KEY (created_by, store_id) REFERENCES app_users(id, store_id) ON DELETE SET NULL (created_by);

COMMIT;
