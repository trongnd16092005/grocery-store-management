# PostgreSQL database

Database: `grocery_store`

## Run locally with pgAdmin

1. Open Query Tool on `grocery_store`.
2. If the tab is connected as `postgres`, run `SET ROLE grocery_app;` first.
3. Open and execute `migrations/V1__create_schema.sql`.
4. Open and execute `migrations/V2__seed_demo_data.sql`.
5. Open and execute `migrations/V3__business_code_sequences.sql`.
6. Open and execute `migrations/V4__add_user_auth_version.sql`.
7. Open and execute `migrations/V5__seed_additional_products.sql`.
8. Open and execute `migrations/V6__multi_store_tenancy.sql`.
9. Open and execute `migrations/V7__enforce_tenant_foreign_keys.sql`.
10. Open and execute `migrations/V8__purchase_orders_discount_approval.sql`.
11. Open and execute `migrations/V9__discount_codes.sql`.
12. Open and execute `migrations/V10__super_admin.sql`.
13. Verify with:

```sql
SELECT current_database(), current_user;
SELECT COUNT(*) FROM categories;
SELECT COUNT(*) FROM products;
SELECT COUNT(*) FROM customers;
SELECT * FROM low_stock_products ORDER BY code;
```

Expected demo counts: 4 categories, 8 products, 6 customers.

## JDBC configuration

Use environment variables instead of committing passwords:

```text
DB_URL=jdbc:postgresql://localhost:5432/grocery_store
DB_USER=grocery_app
DB_PASSWORD=<your local password>
```
