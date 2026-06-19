BEGIN;

INSERT INTO categories (code, name, description) VALUES
    ('DM001', 'Nước ngọt', 'Các loại nước giải khát đóng chai, lon'),
    ('DM002', 'Bánh kẹo', 'Bánh ngọt, bánh quy và các loại kẹo'),
    ('DM003', 'Mì ăn liền', 'Mì gói, cháo và phở ăn liền'),
    ('DM004', 'Gia vị', 'Nước mắm, muối, đường và hạt nêm')
ON CONFLICT (code) DO NOTHING;

INSERT INTO suppliers (code, name, phone, email, address) VALUES
    ('NCC001', 'Nhà phân phối Nước giải khát Việt', '028 3822 1100', 'contact@nuocgiaikhatviet.vn', 'TP. Hồ Chí Minh'),
    ('NCC002', 'Công ty Thực phẩm Tiện lợi', '028 3933 2200', 'sales@thucphamtienloi.vn', 'TP. Hồ Chí Minh'),
    ('NCC003', 'Nhà phân phối Bánh kẹo Việt', '028 3555 3300', 'order@banhkeoviet.vn', 'Bình Dương')
ON CONFLICT (code) DO NOTHING;

INSERT INTO products (
    code, barcode, name, category_id, supplier_id,
    cost_price, selling_price, stock_quantity, minimum_stock, unit
)
SELECT values_table.code,
       values_table.barcode,
       values_table.name,
       c.id,
       s.id,
       values_table.cost_price,
       values_table.selling_price,
       values_table.stock_quantity,
       values_table.minimum_stock,
       values_table.unit
FROM (VALUES
    ('SP001', '8934588012228', 'Coca Cola 320ml',             'DM001', 'NCC001',  7500, 10000, 120, 20, 'lon'),
    ('SP002', '8934680015196', 'Bánh Quy Cosy 200g',          'DM002', 'NCC003', 28000, 35000,   8, 10, 'gói'),
    ('SP003', '8934563138165', 'Mì Hảo Hảo Tôm Chua Cay',    'DM003', 'NCC002',  3500,  4500,   0, 30, 'gói'),
    ('SP004', '8934588590306', 'Aquafina 500ml',              'DM001', 'NCC001',  4000,  6000,  85, 20, 'chai'),
    ('SP005', '9002975379347', 'Kẹo dẻo Haribo 80g',          'DM002', 'NCC003', 17000, 22000,   4, 10, 'gói'),
    ('SP006', '8934564600357', 'Nước mắm Chinsu 500ml',       'DM004', 'NCC002', 36000, 45000,  30, 10, 'chai'),
    ('SP007', '8934563523121', 'Hạt nêm Knorr 400g',          'DM004', 'NCC002', 30000, 38000,  50, 15, 'gói'),
    ('SP008', '8934588013010', 'Pepsi Lon 330ml',             'DM001', 'NCC001',  7000,  9500,  60, 20, 'lon')
) AS values_table(
    code, barcode, name, category_code, supplier_code,
    cost_price, selling_price, stock_quantity, minimum_stock, unit
)
JOIN categories c ON c.code = values_table.category_code
JOIN suppliers s ON s.code = values_table.supplier_code
WHERE TRUE
ON CONFLICT (code) DO NOTHING;

INSERT INTO customers (
    code, full_name, phone, email, gender, address, customer_type
) VALUES
    ('KH001', 'Nguyễn Văn An',   '0901234567', 'an.nguyen@email.com',   'MALE',   'Quận 1, TP. Hồ Chí Minh',          'LOYAL'),
    ('KH002', 'Trần Thị Bình',   '0912345678', 'binh.tran@email.com',   'FEMALE', 'Quận 3, TP. Hồ Chí Minh',          'LOYAL'),
    ('KH003', 'Lê Minh Châu',    '0987654321', NULL,                    'MALE',   'Quận Bình Thạnh, TP. Hồ Chí Minh', 'REGULAR'),
    ('KH004', 'Phạm Hoàng Dũng', '0933456789', NULL,                    'MALE',   'TP. Thủ Đức, TP. Hồ Chí Minh',     'REGULAR'),
    ('KH005', 'Võ Ngọc Hà',      '0908111222', 'ha.vo@email.com',       'FEMALE', 'Quận 7, TP. Hồ Chí Minh',          'REGULAR'),
    ('KH006', 'Đặng Thu Lan',    '0977888999', NULL,                    'FEMALE', 'Quận Tân Bình, TP. Hồ Chí Minh',   'REGULAR')
ON CONFLICT (code) DO NOTHING;

COMMIT;
