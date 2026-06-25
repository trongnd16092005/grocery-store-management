# Grocery Store Management

Ứng dụng quản lý cửa hàng Java Web sử dụng Jakarta Servlet/JSP, PostgreSQL và Maven.

## Chức năng chính

- Đăng ký và vận hành nhiều cửa hàng độc lập trên cùng hệ thống.
- Quản lý sản phẩm, danh mục, khách hàng và tồn kho.
- Nhập hàng, điều chỉnh tồn, cảnh báo tồn tối thiểu và lịch sử kho.
- Bán hàng POS, thanh toán tiền mặt hoặc QR payOS tự động.
- Lưu, tra cứu và hủy hóa đơn; hủy hóa đơn tự động hoàn kho.
- Đăng nhập BCrypt và phân quyền `ADMIN`/`CASHIER`.
- Quản lý tài khoản: tạo, sửa, khóa/mở khóa, đổi mật khẩu (chính mình) và đặt lại mật khẩu (ADMIN).
- Dashboard lấy dữ liệu trực tiếp từ PostgreSQL, có biểu đồ và cảnh báo sắp hết hàng.
- Tích điểm khách hàng, dùng điểm giảm giá khi bán hàng.

## Yêu cầu

- JDK 17 hoặc 21.
- Apache Tomcat 10.1.x. Không dùng Tomcat 9 vì project sử dụng namespace `jakarta.*`.
- PostgreSQL 17 hoặc phiên bản tương thích.
- IntelliJ IDEA Ultimate để cấu hình Tomcat trực tiếp.

## Quick setup cho người mới pull code

Nếu chỉ cần chạy nhanh trên máy khác, làm theo thứ tự này:

1. Cài **JDK 17+**, **Tomcat 10.1.x**, **PostgreSQL 17+** và **Maven**.
2. Clone project:

```bash
git clone <repo-url>
cd grocery-store-management
```

3. Tạo database PostgreSQL:

```sql
CREATE ROLE grocery_app WITH LOGIN PASSWORD '<YOUR_DB_PASSWORD>';
CREATE DATABASE grocery_store
    WITH OWNER grocery_app
    ENCODING 'UTF8';
```

4. Chạy toàn bộ file trong `database/migrations/` theo thứ tự `V1` đến `V16`.
5. Nếu muốn có dữ liệu demo đẹp cho dashboard/phiếu nhập, chạy thêm:

```text
database/demo/seed_dashboard_20260619_20260626.sql
database/demo/seed_purchase_orders_20260618.sql
```

6. Build project:

```bash
mvn clean package
```

7. Chạy bằng Tomcat 10.1 với context:

```text
/grocery-store
```

và VM options tối thiểu:

```text
-Ddb.url=jdbc:postgresql://localhost:5432/grocery_store -Ddb.user=grocery_app -Ddb.password=<YOUR_DB_PASSWORD>
```

8. Mở:

```text
http://localhost:8080/grocery-store/login
```

Nếu port 8080 bận, đổi Tomcat sang 8081 và mở:

```text
http://localhost:8081/grocery-store/login
```

Tài khoản demo sau khi chạy migration/seed mặc định:

| Mã cửa hàng | Tài khoản | Mật khẩu | Vai trò |
|---|---|---|---|
| `CUAHANGABC` | `admin` | `123456` | ADMIN cửa hàng |
| `SYSTEM` | `admin` | `123456` | Super Admin |

Nếu đăng nhập `SYSTEM/admin/123456`, hệ thống có thể yêu cầu đổi mật khẩu trước khi dùng.

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
4. `database/migrations/V4__add_user_auth_version.sql`
5. `database/migrations/V5__seed_additional_products.sql`
6. `database/migrations/V6__multi_store_tenancy.sql`
7. `database/migrations/V7__enforce_tenant_foreign_keys.sql`
8. `database/migrations/V8__purchase_orders_discount_approval.sql`
9. `database/migrations/V9__discount_codes.sql`
10. `database/migrations/V10__super_admin.sql`
11. `database/migrations/V11__single_super_admin.sql`
12. `database/migrations/V12__payos_qr_payments.sql`
13. `database/migrations/V13__store_payos_settings.sql`
14. `database/migrations/V14__customer_loyalty_discount_conditions.sql`
15. `database/migrations/V15__fix_discount_product_fk_delete.sql`
16. `database/migrations/V16__redeemable_loyalty_points.sql`

Không chạy migration trên database mặc định `postgres`.

Có thể kiểm tra dữ liệu demo bằng:

```sql
SELECT COUNT(*) FROM categories;
SELECT COUNT(*) FROM products;
SELECT COUNT(*) FROM customers;
```

Sau khi chạy đầy đủ V1–V16, database có đủ schema, dữ liệu nền, phân quyền,
QR payOS, tích điểm khách hàng và các cột phục vụ dashboard hiện tại.

Nếu muốn dữ liệu dashboard đẹp hơn để demo, chạy thêm hai file demo:

```text
database/demo/seed_dashboard_20260619_20260626.sql
database/demo/seed_purchase_orders_20260618.sql
```

Các file demo này tạo dữ liệu liên thông giữa hóa đơn, khách hàng, tồn kho,
phiếu nhập và lịch sử kho. Chỉ chạy trên database demo/phát triển, không chạy trên dữ liệu thật.

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
DB_POOL_MAX_SIZE=6
DB_POOL_MIN_IDLE=1
PAYOS_CLIENT_ID=<PAYOS_CLIENT_ID>
PAYOS_API_KEY=<PAYOS_API_KEY>
PAYOS_CHECKSUM_KEY=<PAYOS_CHECKSUM_KEY>
PAYOS_TEST_AMOUNT=5000
APP_BASE_URL=https://your-public-domain.example/grocery-store
```

Nếu chưa test QR payOS, vẫn có thể chạy app bằng tiền mặt. Khi bấm QR mà thiếu
key/payOS hoặc chưa bật setup QR, hệ thống sẽ báo lỗi cấu hình.

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

Mỗi cửa hàng đăng ký tại `/register`. Hệ thống tạo cửa hàng và tài khoản ADMIN đầu tiên trong cùng một transaction; mật khẩu được băm BCrypt trước khi lưu.

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

## Chạy bằng Docker

Docker là cách gọn nhất để chạy app trên máy khác hoặc deploy lên Render/Railway/VPS
mà không phải cấu hình Tomcat thủ công.

Build image:

```bash
docker build -t grocery-store-management .
```

Chạy container:

```bash
docker run --rm -p 8081:8080 ^
  -e DB_URL="jdbc:postgresql://host:5432/grocery_store?sslmode=require" ^
  -e DB_USER="grocery_app" ^
  -e DB_PASSWORD="<DB_PASSWORD>" ^
  -e APP_BASE_URL="http://localhost:8081/grocery-store" ^
  -e PAYOS_CLIENT_ID="<PAYOS_CLIENT_ID>" ^
  -e PAYOS_API_KEY="<PAYOS_API_KEY>" ^
  -e PAYOS_CHECKSUM_KEY="<PAYOS_CHECKSUM_KEY>" ^
  grocery-store-management
```

Mở:

```text
http://localhost:8081/grocery-store/login
```

Hoặc dùng Docker Compose:

```bash
copy deployment.env.example .env
copy docker-compose.example.yml docker-compose.yml
docker compose up --build
```

Không commit file `.env`, mật khẩu database hoặc key payOS thật.

Ứng dụng sử dụng HikariCP để tái sử dụng kết nối PostgreSQL. Với database
Render, cấu hình mặc định `DB_POOL_MAX_SIZE=6` và `DB_POOL_MIN_IDLE=1`
phù hợp cho giai đoạn phát triển và cửa hàng nhỏ. Không nên tăng pool tùy ý
vì mỗi gói PostgreSQL có giới hạn số kết nối.

Trong giai đoạn test payOS, `PAYOS_TEST_AMOUNT=5000` khiến QR tự động tạo
giao dịch 5.000đ để tránh phải thanh toán thử các hóa đơn lớn. Khi chạy thật,
đặt `PAYOS_TEST_AMOUNT=0` để dùng đúng tổng tiền hóa đơn.

Lưu ý: ở chế độ test, `payment_transactions.amount` là 5.000đ để payOS/webhook
đối chiếu, còn `invoices.total_amount` vẫn lưu đúng tổng tiền hóa đơn thật.

## Phân quyền

- `SUPER_ADMIN`: quản lý danh sách cửa hàng, khóa/mở khóa cửa hàng và tự đổi mật khẩu;
  không truy cập dữ liệu bán hàng, kho hoặc khách hàng của từng tenant.
- `ADMIN`: dashboard, sản phẩm, danh mục, kho, tài khoản, khách hàng, bán hàng, xem và hủy hóa đơn.
- `CASHIER`: dashboard, khách hàng, bán hàng, xem hóa đơn và tự đổi mật khẩu; không được quản lý kho hoặc hủy hóa đơn.

Khi tài khoản bị khóa, đổi vai trò hoặc đổi/reset mật khẩu, mọi session cũ của tài khoản đó sẽ bị vô hiệu hóa ở request tiếp theo. Hệ thống cũng ngăn tự hạ quyền, tự khóa và ngăn khóa/hạ quyền quản trị viên đang hoạt động cuối cùng.

## Mô hình nhiều cửa hàng

- Sau khi chạy migration V11, hệ thống chỉ có đúng một Super Admin.
- Đăng nhập lần đầu bằng mã cửa hàng `SYSTEM`, tài khoản `admin`,
  mật khẩu `123456`; hệ thống bắt buộc đổi mật khẩu trước khi sử dụng.
- Không có màn hình hoặc API tạo thêm Super Admin. Database cũng chặn tạo,
  đổi vai trò, khóa hoặc xóa tài khoản Super Admin duy nhất.
- Trang `/super-admin` cho phép xem số ADMIN/nhân viên và khóa hoặc mở khóa cửa hàng.
- Khi cửa hàng bị khóa, đăng nhập mới bị từ chối và các session hiện có hết hiệu lực
  ở request tiếp theo.
- Truy cập `/register` để tạo cửa hàng và ADMIN đầu tiên.
- Khi đăng nhập cần nhập `mã cửa hàng`, tên đăng nhập và mật khẩu.
- Mỗi cửa hàng có thể tạo nhiều ADMIN và CASHIER trong mục Tài khoản.
- Sản phẩm, danh mục, nhà cung cấp, khách hàng, kho, phiếu nhập và hóa đơn đều thuộc một `store_id`.
- PostgreSQL Row-Level Security tự giới hạn mọi truy vấn theo cửa hàng hiện tại.
- Khóa ngoại kép `(id, store_id)` ngăn liên kết dữ liệu giữa hai cửa hàng ngay cả khi request giả mạo ID.
- Dữ liệu cũ được giữ trong cửa hàng mặc định có mã `CUAHANGABC`.

Thanh toán POS và hủy hóa đơn chạy trong transaction PostgreSQL, khóa bản ghi tồn kho và ghi lịch sử `SALE`/`CANCEL_SALE`.

## Thanh toán QR tự động với payOS

Sau khi chạy migration V12 và V13, cấu hình URL công khai cho app:

```text
PAYOS_CLIENT_ID=<client-id>
PAYOS_API_KEY=<api-key>
PAYOS_CHECKSUM_KEY=<checksum-key>
APP_BASE_URL=https://your-public-domain.example/grocery-store
```

`PAYOS_*` là cấu hình fallback toàn deployment. ADMIN từng cửa hàng có thể nhập
bộ key riêng trong `Thông tin cửa hàng → Setup QR`; POS sẽ ưu tiên dùng key của
cửa hàng đó. `APP_BASE_URL` phải là URL HTTPS công khai và đã bao gồm application context.
Webhook cần đăng ký trên payOS:

```text
https://your-public-domain.example/grocery-store/api/payments/payos/webhook
```

Luồng QR:

1. POS tạo hóa đơn `PENDING`, giữ tồn kho và gọi API tạo link payOS.
2. POS hiển thị QR, số tiền và tự kiểm tra trạng thái mỗi 2,5 giây.
3. Webhook hợp lệ chuyển hóa đơn sang `PAID`; chữ ký HMAC-SHA256, số tiền
   và mã giao dịch đều được kiểm tra.
4. Webhook gửi lặp không trừ kho lần thứ hai.
5. Khi hủy, lỗi hoặc hết hạn, hàng đang giữ được hoàn lại tự động.
6. Webhook đến sau khi giao dịch đã hủy/hết hạn được đưa vào trạng thái
   `REVIEW`, không tự động ghi nhận bán hàng.

Không đặt key payOS trong JSP, JavaScript, repository hoặc URL. Sau khi thay
đổi key fallback cần khởi động lại Tomcat/container. Với key nhập trong tab
Setup QR, chỉ cần lưu cấu hình cửa hàng.

## Checklist sau khi setup

Sau khi chạy app trên máy mới, kiểm tra nhanh các luồng sau:

1. Đăng nhập `CUAHANGABC / admin / 123456`.
2. Vào Dashboard, kiểm tra biểu đồ doanh thu và cảnh báo tồn kho.
3. Vào Sản phẩm, Danh mục, Nhà cung cấp, Kho, Phiếu nhập để chắc trang load 200.
4. Vào Bán hàng:
   - sản phẩm hiện skeleton rồi load danh sách;
   - thêm sản phẩm vào giỏ;
   - thanh toán tiền mặt;
   - nếu có key payOS, thử QR test 5.000đ.
5. Vào Hóa đơn, mở chi tiết và in hóa đơn.
6. Vào Khách hàng, kiểm tra điểm khả dụng/tích lũy.
7. Vào Thông tin cửa hàng, kiểm tra setup QR nếu cần.
8. Đăng xuất rồi thử `SYSTEM / admin / 123456` để kiểm tra Super Admin.

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

### Đăng nhập đúng mật khẩu nhưng vẫn không vào được

Kiểm tra:

- đã nhập đúng **mã cửa hàng** chưa, ví dụ `CUAHANGABC`;
- database đang kết nối đúng DB đã chạy migration chưa;
- tài khoản/cửa hàng có bị khóa bởi Super Admin không;
- nếu Tomcat vừa redeploy, hãy đăng nhập lại vì session cũ có thể mất.

### Trang vẫn hiện giao diện cũ

Thử `Ctrl + F5`. Nếu vẫn không đổi, restart/redeploy Tomcat vì JSP/CSS có thể
đang được server hoặc trình duyệt cache.

### QR chỉ hiện 5.000đ

Đây là chế độ test payOS. Hóa đơn vẫn lưu đúng tổng tiền thật. Muốn QR tạo đúng
tổng tiền hóa đơn, đặt:

```text
PAYOS_TEST_AMOUNT=0
```

rồi restart Tomcat/container nếu dùng biến môi trường hoặc VM option fallback.

## Lịch sử thay đổi

### 2026-06-23 (lần 2)

- **Quản lý tài khoản đầy đủ** (`UserDao`, `JdbcUserDao`, `AuthService`, `UserServlet`, `users.jsp`):
  - **Sửa thông tin**: ADMIN chỉnh họ tên và vai trò của bất kỳ tài khoản nào qua modal "Chỉnh sửa tài khoản".
  - **Khóa / Mở khóa**: ADMIN khoá hoặc mở khoá tài khoản bằng nút 🔒/🔓 trên bảng danh sách; không thể khoá chính tài khoản đang đăng nhập.
  - **Đổi mật khẩu (chính mình)**: mọi người dùng có thể đổi mật khẩu qua modal "Đổi mật khẩu"; yêu cầu xác nhận mật khẩu hiện tại và nhập lại mật khẩu mới để tránh nhầm lẫn.
  - **Đặt lại mật khẩu (ADMIN)**: ADMIN đặt lại mật khẩu cho bất kỳ tài khoản nào mà không cần mật khẩu cũ, thông qua modal "Đặt lại mật khẩu".
  - `UserDao` bổ sung `findById`, `update`, `setActive`, `updatePassword`; `JdbcUserDao` implement đầy đủ bằng `RETURNING *` và parameterized query.
  - `UserServlet.doPost` phân nhánh theo tham số ẩn `action` (`edit` / `lock` / `unlock` / `change-password` / `reset-password` / trống = tạo mới); tất cả thao tác quản trị đều kiểm tra `ADMIN` role trước khi thực thi.
  - Mọi xác thực mật khẩu dùng BCrypt; mật khẩu mới tối thiểu 8 ký tự.
  - Sidebar bổ sung truyền `currentUserId` và `isAdmin` vào JSP để ẩn/hiện nút theo quyền.

### 2026-06-23

- Xoá các file HTML prototype cũ ở `src/main/webapp/`; mọi màn hình nghiệp vụ hiện được phục vụ qua Servlet + JSP trong `WEB-INF/views`.
- POS được phục vụ tại route `/sale` bởi `SaleServlet` và `sale.jsp`; JavaScript trong JSP gọi `/api/products/sale`, `/api/checkout`, `/api/customers/lookup`.
- **Thêm giảm giá hóa đơn** (cột `discount_amount` đã có sẵn trong bảng `invoices` nhưng chưa được dùng):
  - `sale.jsp`: thêm ô nhập giảm giá theo **%** hoặc **số tiền (đ)** trong khung tóm tắt giỏ hàng, hiển thị Tạm tính / Giảm giá / Tổng tiền riêng biệt; tiền thối và điều kiện thanh toán tiền mặt được tính lại theo tổng tiền sau giảm giá.
  - POS nhập mã khuyến mãi và backend tự kiểm tra điều kiện trước khi thanh toán.
  - `InvoiceDao` / `JdbcInvoiceDao`: số tiền giảm giá được **tính lại ở backend**
    dựa trên subtotal đã chốt từ DB, không tin số tiền phía giao diện gửi lên.

### 2026-06-25

- **Nhà cung cấp**: ADMIN quản lý tại `/suppliers`, gồm thêm, sửa, tìm kiếm,
  khóa/mở khóa và xem số sản phẩm đang liên kết. Dữ liệu được tách theo từng cửa hàng.
- **Thông tin cửa hàng**: ADMIN cập nhật tên, số điện thoại và địa chỉ tại `/store`.
  Mã cửa hàng được giữ cố định vì được dùng trong quá trình đăng nhập.
- **Phiếu nhập nhiều sản phẩm**: tạo phiếu nháp tại `/purchase-orders`, sau đó
  hoàn thành để cộng tồn hoặc hủy để hoàn tác tồn kho.
- **Mã giảm giá**: ADMIN quản lý tại `/discount-codes`; hỗ trợ giảm theo phần trăm
  hoặc số tiền, đơn tối thiểu, mức giảm tối đa, thời hạn và giới hạn lượt sử dụng.
  POS chỉ nhận mã hợp lệ và hóa đơn lưu lại mã đã dùng.
- **In hóa đơn**: trang `/invoices/print?id=...` cung cấp mẫu receipt 80 mm lấy
  trực tiếp dữ liệu hóa đơn đã lưu trong PostgreSQL.

## Việc còn có thể cải thiện

- Tách bớt CSS inline khỏi một số JSP, đặc biệt màn POS.
- Thêm unit/integration test tự động cho checkout, QR, điểm khách hàng và phiếu nhập.
- Thêm trang health check DB/payOS để hỗ trợ deploy production.
- Cho phép cấu hình tỷ lệ tích điểm/quy đổi điểm trực tiếp trong giao diện cửa hàng.
