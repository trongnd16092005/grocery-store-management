BEGIN;

INSERT INTO products (
    code, barcode, name, category_id, supplier_id,
    cost_price, selling_price, stock_quantity, minimum_stock, unit
)
SELECT v.code, v.barcode, v.name, c.id, s.id,
       v.cost_price, v.selling_price, v.stock_quantity, v.minimum_stock, v.unit
FROM (VALUES
    ('SP009', '8934588014505', '7Up Lon 330ml',                 'DM001', 'NCC001',  7000,  9500,  75, 20, 'lon'),
    ('SP010', '8934588053054', 'Sting Dâu 330ml',               'DM001', 'NCC001',  7800, 11000,  48, 15, 'chai'),
    ('SP011', '8935049501244', 'Trà xanh C2 360ml',             'DM001', 'NCC001',  6500,  9000,  36, 15, 'chai'),
    ('SP012', '8934680034227', 'Bánh Oreo Vanilla 133g',        'DM002', 'NCC003', 14000, 19000,  25, 10, 'gói'),
    ('SP013', '8934563141202', 'Bánh ChocoPie 6 cái',           'DM002', 'NCC003', 31000, 39000,  18,  8, 'hộp'),
    ('SP014', '8936036021318', 'Kẹo Mentos Bạc Hà',             'DM002', 'NCC003',  6500, 10000,  40, 12, 'thỏi'),
    ('SP015', '8934563138202', 'Mì Hảo Hảo Sa Tế Hành',        'DM003', 'NCC002',  3500,  4500,  65, 30, 'gói'),
    ('SP016', '8936017360320', 'Mì Omachi Sườn Hầm Ngũ Quả',   'DM003', 'NCC002',  6500,  8500,  24, 15, 'gói'),
    ('SP017', '8934563651138', 'Mì Kokomi Đại Tôm Chua Cay',   'DM003', 'NCC002',  3200,  4000,  50, 25, 'gói'),
    ('SP018', '8934564601026', 'Nước tương Chinsu 250ml',       'DM004', 'NCC002', 13000, 18000,  22, 10, 'chai'),
    ('SP019', '8934868140221', 'Đường tinh luyện Biên Hòa 1kg','DM004', 'NCC002', 21000, 27000,  16, 10, 'gói'),
    ('SP020', '8934563526207', 'Muối i-ốt Vifon 500g',          'DM004', 'NCC002',  4500,  7000,  35, 12, 'gói')
) AS v(
    code, barcode, name, category_code, supplier_code,
    cost_price, selling_price, stock_quantity, minimum_stock, unit
)
JOIN categories c ON c.code = v.category_code
JOIN suppliers s ON s.code = v.supplier_code
ON CONFLICT (code) DO NOTHING;

SELECT setval(
    'product_code_seq',
    COALESCE((SELECT MAX(SUBSTRING(code FROM 3)::BIGINT) FROM products), 0) + 1,
    FALSE
);

COMMIT;
