BEGIN;

ALTER TABLE app_users DROP CONSTRAINT IF EXISTS chk_app_users_role;
ALTER TABLE app_users ADD CONSTRAINT chk_app_users_role
    CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'CASHIER'));

INSERT INTO stores(code, name, active)
VALUES ('SYSTEM', 'Quản trị hệ thống', TRUE)
ON CONFLICT (code) DO UPDATE
SET name=EXCLUDED.name, active=TRUE;

COMMIT;
