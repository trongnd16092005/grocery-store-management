# Grocery Store Management

Ứng dụng quản lý cửa hàng Java Web sử dụng Jakarta Servlet/JSP, PostgreSQL và Maven.

## Chức năng chính

- Quản lý sản phẩm, danh mục, khách hàng và tồn kho.
- Nhập hàng, điều chỉnh tồn, cảnh báo tồn tối thiểu và lịch sử kho.
- Bán hàng POS, thanh toán tiền mặt hoặc giao diện QR dự phòng.
- Lưu, tra cứu và hủy hóa đơn; hủy hóa đơn tự động hoàn kho.
- Đăng nhập BCrypt và phân quyền `ADMIN`/`CASHIER`.
- Dashboard lấy dữ liệu trực tiếp từ PostgreSQL.

## Yêu cầu

- JDK 17 hoặc 21.
- Apache Tomcat 10.1.x. Không dùng Tomcat 9 vì project sử dụng namespace `jakarta.*`.
- PostgreSQL 17 hoặc phiên bản tương thích.
- IntelliJ IDEA Ultimate để cấu hình Tomcat trực tiếp.

## 1. Khởi tạo PostgreSQL

Trong pgAdmin, tạo login role `grocery_app` và database `grocery_store` do role này sở hữu. Ví dụ SQL:

```sql
CREATE ROLE grocery_app WITH LOGIN PASSWORD '<YOUR_DB_PASSWORD>';
CREATE DATABASE grocery_store
    WITH OWNER grocery_app
    ENCODING 'UTF8';
```

Nếu role hoặc database đã tồn tại thì không chạy lại hai lệnh trên.

Kết nối Query Tool vào chính database `grocery_store`, sau đó chạy migration theo đúng thứ tự:

1. `database/migrations/V1__create_schema.sql`
2. `database/migrations/V2__seed_demo_data.sql`
3. `database/migrations/V3__business_code_sequences.sql`

Không chạy migration trên database mặc định `postgres`.

Có thể kiểm tra dữ liệu demo bằng:

```sql
SELECT COUNT(*) FROM categories;
SELECT COUNT(*) FROM products;
SELECT COUNT(*) FROM customers;
```

Kết quả seed hiện tại tương ứng là 4 danh mục, 8 sản phẩm và 6 khách hàng.

## 2. Thêm Tomcat vào IntelliJ

1. Giải nén Apache Tomcat 10.1.x.
2. Mở `File → Settings`.
3. Chọn `Build, Execution, Deployment → Application Servers`.
4. Nhấn `+ → Tomcat Server`.
5. Chọn thư mục Tomcat vừa giải nén.

## 3. Tạo Run Configuration

1. Mở `Run → Edit Configurations`.
2. Chọn `+ → Tomcat Server → Local`.
3. Chọn Tomcat 10.1 vừa cấu hình.
4. Chọn JRE 17 hoặc 21.
5. Để HTTP port là `8080`.
6. Điền một dòng sau vào ô **VM options**:

```text
-Ddb.url=jdbc:postgresql://localhost:5432/grocery_store -Ddb.user=grocery_app -Ddb.password=<YOUR_DB_PASSWORD>
```

Không ghi mật khẩu thật vào README, source code hoặc commit Git.

Project cũng hỗ trợ các biến môi trường tương đương:

```text
DB_URL=jdbc:postgresql://localhost:5432/grocery_store
DB_USER=grocery_app
DB_PASSWORD=<YOUR_DB_PASSWORD>
```

## 4. Thêm artifact để deploy

Trong Run Configuration của Tomcat:

1. Mở tab `Deployment`.
2. Nhấn `+ → Artifact`.
3. Chọn `RetailStoreManagement:war exploded`.
4. Đặt `Application context` là `/grocery-store`.
5. Quay lại tab `Server` và đặt URL:

```text
http://localhost:8080/grocery-store/
```

Nếu chưa có artifact:

1. Mở `File → Project Structure → Artifacts`.
2. Chọn `+ → Web Application: Exploded → From Modules`.
3. Chọn module `RetailStoreManagement`.

## 5. Chạy ứng dụng

Nhấn Run ở cấu hình Tomcat và mở:

```text
http://localhost:8080/grocery-store/
```

Lần chạy đầu, nếu bảng `app_users` chưa có tài khoản, ứng dụng chuyển tới `/setup`. Tạo tài khoản quản trị viên tại đây; mật khẩu được băm BCrypt trước khi lưu. Sau khi có tài khoản đầu tiên, trang setup tự khóa.

## Build WAR bằng Maven

Có thể build độc lập với IntelliJ:

```text
mvn clean package
```

File kết quả:

```text
target/RetailStoreManagement-1.0-SNAPSHOT.war
```

Thư mục `target/` là kết quả build và không được đưa lên Git.

## Phân quyền

- `ADMIN`: dashboard, sản phẩm, danh mục, kho, tài khoản, khách hàng, bán hàng, xem và hủy hóa đơn.
- `CASHIER`: dashboard, khách hàng, bán hàng và xem hóa đơn; không được quản lý kho hoặc hủy hóa đơn.

Thanh toán POS và hủy hóa đơn chạy trong transaction PostgreSQL, khóa bản ghi tồn kho và ghi lịch sử `SALE`/`CANCEL_SALE`.

## Xử lý lỗi thường gặp

### Port 8080 đã được sử dụng

Kiểm tra tiến trình chiếm cổng trong PowerShell:

```powershell
Get-NetTCPConnection -LocalPort 8080 -State Listen |
    Select-Object LocalAddress, LocalPort, OwningProcess
```

Sau đó dừng đúng terminal/server cũ hoặc đổi HTTP port của Tomcat sang `8081`.

### `No artifacts marked for deployment`

Mở tab `Deployment` và thêm `RetailStoreManagement:war exploded`.

### `ClassNotFoundException: jakarta.servlet...`

Đang dùng Tomcat 9 hoặc cũ hơn. Chuyển sang Tomcat 10.1.x.

### Thiếu `DB_PASSWORD`

Thêm `-Ddb.password=...` vào VM options của Tomcat, sau đó Stop và Run lại server.

### `relation products does not exist`

Migration chưa được chạy hoặc đã chạy nhầm trên database `postgres`. Hãy mở Query Tool của database `grocery_store` và chạy lại đúng thứ tự.

### Lỗi CSRF hoặc “Phiên biểu mẫu không hợp lệ”

Tải lại trang và đăng nhập lại nếu phiên đã hết hạn.
