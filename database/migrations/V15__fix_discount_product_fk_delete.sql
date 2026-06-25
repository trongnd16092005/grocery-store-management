BEGIN;

ALTER TABLE discount_codes DROP CONSTRAINT IF EXISTS fk_discount_product_store;
ALTER TABLE discount_codes ADD CONSTRAINT fk_discount_product_store
    FOREIGN KEY (product_id, store_id)
    REFERENCES products(id, store_id)
    ON DELETE SET NULL (product_id);

COMMIT;
