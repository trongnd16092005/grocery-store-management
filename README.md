# Grocery Store Management

Ứng dụng quản lý cửa hàng Java Web dùng Jakarta EE 10, PostgreSQL, JSP/Servlet và Maven.

## Chạy local

1. Tạo database và chạy lần lượt các file trong `database/migrations`.
2. Khai báo biến môi trường:

   - `DB_URL=jdbc:postgresql://localhost:5432/grocery_store`
   - `DB_USER=grocery_app`
   - `DB_PASSWORD=<mật khẩu PostgreSQL của máy>`

3. Đóng gói bằng `mvn clean package`.
4. Deploy file `target/RetailStoreManagement-1.0-SNAPSHOT.war` lên server hỗ trợ Jakarta EE 10, ví dụ Payara 6.
5. Lần đầu truy cập, ứng dụng chuyển tới `/setup` để tạo quản trị viên. Màn hình này tự khóa sau khi tài khoản đầu tiên được tạo.

Không đưa mật khẩu hoặc file `.env` lên Git. Trên hosting, cấu hình ba biến DB trong phần Environment/Secrets của dịch vụ.

## Quyền truy cập

- `ADMIN`: sản phẩm, danh mục, kho, tài khoản, khách hàng, bán hàng và hủy hóa đơn.
- `CASHIER`: khách hàng, bán hàng và xem hóa đơn; không thể hủy hóa đơn.

Thanh toán POS và hủy hóa đơn đều chạy trong transaction PostgreSQL, có khóa tồn kho và ghi lịch sử kho.
