BEGIN;

ALTER TABLE app_users
    ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

INSERT INTO stores(code, name, active)
VALUES ('SYSTEM', 'Quản trị hệ thống', TRUE)
ON CONFLICT (code) DO UPDATE
SET name=EXCLUDED.name, active=TRUE;

SELECT set_config(
    'app.current_store_id',
    (SELECT id::TEXT FROM stores WHERE code='SYSTEM'),
    FALSE
);

DO $$
DECLARE
    system_store_id BIGINT;
    canonical_user_id BIGINT;
    tenant RECORD;
BEGIN
    SELECT id INTO system_store_id FROM stores WHERE code='SYSTEM';

    SELECT id INTO canonical_user_id
    FROM app_users
    WHERE store_id=system_store_id
      AND (LOWER(username)='admin' OR role='SUPER_ADMIN')
    ORDER BY CASE WHEN LOWER(username)='admin' THEN 0 ELSE 1 END, id
    LIMIT 1;

    IF canonical_user_id IS NULL THEN
        INSERT INTO app_users(
            store_id, username, password_hash, full_name, role, active,
            must_change_password
        )
        VALUES (
            system_store_id,
            'admin',
            '$2a$12$CGus.xLEPKGGhcaOWksPBevLTyK/aR5fy5qu9/dfEDvb9oNG6DjHa',
            'Quản trị hệ thống',
            'SUPER_ADMIN',
            TRUE,
            TRUE
        )
        RETURNING id INTO canonical_user_id;
    ELSE
        DELETE FROM app_users
        WHERE store_id=system_store_id
          AND LOWER(username)='admin'
          AND id<>canonical_user_id;

        UPDATE app_users
        SET username='admin',
            password_hash='$2a$12$CGus.xLEPKGGhcaOWksPBevLTyK/aR5fy5qu9/dfEDvb9oNG6DjHa',
            full_name='Quản trị hệ thống',
            role='SUPER_ADMIN',
            active=TRUE,
            must_change_password=TRUE,
            auth_version=auth_version+1
        WHERE id=canonical_user_id;
    END IF;

    DELETE FROM app_users
    WHERE role='SUPER_ADMIN' AND id<>canonical_user_id;

    FOR tenant IN SELECT id FROM stores WHERE id<>system_store_id LOOP
        PERFORM set_config('app.current_store_id', tenant.id::TEXT, FALSE);
        DELETE FROM app_users WHERE role='SUPER_ADMIN';
    END LOOP;

    PERFORM set_config('app.current_store_id', system_store_id::TEXT, FALSE);
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_single_super_admin
    ON app_users ((role))
    WHERE role='SUPER_ADMIN';

CREATE OR REPLACE FUNCTION protect_single_super_admin()
RETURNS TRIGGER AS $$
DECLARE
    system_store_id BIGINT;
BEGIN
    SELECT id INTO system_store_id FROM stores WHERE code='SYSTEM';

    IF TG_OP='DELETE' AND OLD.role='SUPER_ADMIN' THEN
        RAISE EXCEPTION 'Không thể xóa tài khoản Super Admin duy nhất.';
    END IF;

    IF TG_OP='UPDATE' AND OLD.role='SUPER_ADMIN'
       AND (NEW.role<>'SUPER_ADMIN' OR NOT NEW.active OR NEW.store_id<>system_store_id) THEN
        RAISE EXCEPTION 'Không thể đổi vai trò, khóa hoặc chuyển Super Admin khỏi tenant SYSTEM.';
    END IF;

    IF TG_OP<>'DELETE' AND NEW.role='SUPER_ADMIN'
       AND (NEW.store_id<>system_store_id OR NOT NEW.active) THEN
        RAISE EXCEPTION 'Super Admin phải thuộc tenant SYSTEM và luôn hoạt động.';
    END IF;

    IF TG_OP='DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_protect_single_super_admin ON app_users;
CREATE TRIGGER trg_protect_single_super_admin
BEFORE INSERT OR UPDATE OR DELETE ON app_users
FOR EACH ROW EXECUTE FUNCTION protect_single_super_admin();

COMMIT;
