<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đăng ký cửa hàng | Quản lý cửa hàng</title>
    <link
            href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
            rel="stylesheet">
    <link
            href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css"
            rel="stylesheet">
    <link
            href="${pageContext.request.contextPath}/assets/css/login.css"
            rel="stylesheet">
</head>
<body>
<main class="register-single-page">
    <section class="login-panel">
        <div class="login-form-wrap register-form-wrap">
            <header class="login-header">
                <div class="mobile-brand register-brand">
                    <span class="brand-mark" aria-hidden="true">
                        <i class="fa-solid fa-store"></i>
                    </span>
                    <span>Retail Store</span>
                </div>
                <h2>Đăng ký cửa hàng</h2>
                <p>Điền thông tin cơ bản để khởi tạo cửa hàng và tài khoản quản trị đầu tiên.</p>
            </header>

            <c:if test="${not empty error}">
                <div class="alert alert-danger login-alert" role="alert">
                    <i class="fa-solid fa-circle-exclamation" aria-hidden="true"></i>
                    <c:out value="${error}"/>
                </div>
            </c:if>

            <form method="post" class="login-form register-form">
                <input
                        type="hidden"
                        name="csrfToken"
                        value="${sessionScope.csrfToken}">

                <section class="register-section" aria-labelledby="storeInfoTitle">
                    <div class="section-heading">
                        <span class="section-icon" aria-hidden="true">
                            <i class="fa-solid fa-shop"></i>
                        </span>
                        <div>
                            <h3 id="storeInfoTitle">Thông tin cửa hàng</h3>
                            <p>Thông tin này sẽ hiển thị trong hệ thống quản lý.</p>
                        </div>
                    </div>

                    <div class="register-grid">
                        <div class="form-field">
                            <label for="storeCode">Mã cửa hàng</label>
                            <div class="input-wrap">
                                <i class="fa-solid fa-building" aria-hidden="true"></i>
                                <input
                                        id="storeCode"
                                        class="form-control text-uppercase"
                                        name="storeCode"
                                        pattern="[A-Za-z0-9_-]{3,30}"
                                        placeholder="Ví dụ: CUAHANG01"
                                        required
                                        autocomplete="organization">
                            </div>
                            <div class="field-hint">Dùng khi đăng nhập. Từ 3–30 ký tự.</div>
                        </div>

                        <div class="form-field">
                            <label for="storeName">Tên cửa hàng</label>
                            <div class="input-wrap">
                                <i class="fa-solid fa-store" aria-hidden="true"></i>
                                <input
                                        id="storeName"
                                        class="form-control"
                                        name="storeName"
                                        placeholder="Ví dụ: Cửa hàng ABC"
                                        required
                                        autocomplete="organization-title">
                            </div>
                        </div>

                        <div class="form-field">
                            <label for="phone">Điện thoại</label>
                            <div class="input-wrap">
                                <i class="fa-solid fa-phone" aria-hidden="true"></i>
                                <input
                                        id="phone"
                                        class="form-control"
                                        name="phone"
                                        placeholder="Số điện thoại cửa hàng"
                                        autocomplete="tel">
                            </div>
                        </div>

                        <div class="form-field">
                            <label for="address">Địa chỉ</label>
                            <div class="input-wrap">
                                <i class="fa-solid fa-location-dot" aria-hidden="true"></i>
                                <input
                                        id="address"
                                        class="form-control"
                                        name="address"
                                        placeholder="Địa chỉ cửa hàng"
                                        autocomplete="street-address">
                            </div>
                        </div>
                    </div>
                </section>

                <section class="register-section" aria-labelledby="adminInfoTitle">
                    <div class="section-heading">
                        <span class="section-icon" aria-hidden="true">
                            <i class="fa-solid fa-user-shield"></i>
                        </span>
                        <div>
                            <h3 id="adminInfoTitle">Quản trị viên đầu tiên</h3>
                            <p>Tài khoản này có quyền quản lý toàn bộ cửa hàng.</p>
                        </div>
                    </div>

                    <div class="register-grid">
                        <div class="form-field">
                            <label for="fullName">Họ tên</label>
                            <div class="input-wrap">
                                <i class="fa-regular fa-id-card" aria-hidden="true"></i>
                                <input
                                        id="fullName"
                                        class="form-control"
                                        name="fullName"
                                        placeholder="Tên người quản trị"
                                        required
                                        autocomplete="name">
                            </div>
                        </div>

                        <div class="form-field">
                            <label for="username">Tên đăng nhập</label>
                            <div class="input-wrap">
                                <i class="fa-regular fa-user" aria-hidden="true"></i>
                                <input
                                        id="username"
                                        class="form-control"
                                        name="username"
                                        pattern="[A-Za-z0-9._-]{3,50}"
                                        placeholder="Ví dụ: admin"
                                        required
                                        autocomplete="username">
                            </div>
                        </div>

                        <div class="form-field">
                            <label for="password">Mật khẩu</label>
                            <div class="input-wrap password-wrap">
                                <i class="fa-solid fa-lock" aria-hidden="true"></i>
                                <input
                                        id="password"
                                        type="password"
                                        class="form-control"
                                        name="password"
                                        minlength="8"
                                        placeholder="Tối thiểu 8 ký tự"
                                        required
                                        autocomplete="new-password">
                                <button
                                        class="password-toggle"
                                        type="button"
                                        aria-label="Hiện mật khẩu"
                                        aria-pressed="false"
                                        data-target="password">
                                    <i class="fa-regular fa-eye" aria-hidden="true"></i>
                                </button>
                            </div>
                        </div>

                        <div class="form-field">
                            <label for="confirmPassword">Xác nhận mật khẩu</label>
                            <div class="input-wrap password-wrap">
                                <i class="fa-solid fa-shield-halved" aria-hidden="true"></i>
                                <input
                                        id="confirmPassword"
                                        type="password"
                                        class="form-control"
                                        name="confirmPassword"
                                        minlength="8"
                                        placeholder="Nhập lại mật khẩu"
                                        required
                                        autocomplete="new-password">
                                <button
                                        class="password-toggle"
                                        type="button"
                                        aria-label="Hiện mật khẩu"
                                        aria-pressed="false"
                                        data-target="confirmPassword">
                                    <i class="fa-regular fa-eye" aria-hidden="true"></i>
                                </button>
                            </div>
                        </div>
                    </div>
                </section>

                <button class="btn-login" type="submit">
                    Tạo cửa hàng
                    <i class="fa-solid fa-arrow-right" aria-hidden="true"></i>
                </button>
            </form>

            <div class="register-link">
                Đã có cửa hàng?
                <a href="${pageContext.request.contextPath}/login">
                    Đăng nhập
                </a>
            </div>
        </div>
    </section>
</main>

<script>
    document.querySelectorAll('.password-toggle').forEach((button) => {
        button.addEventListener('click', () => {
            const input = document.getElementById(button.dataset.target);
            const showing = input.type === 'text';
            input.type = showing ? 'password' : 'text';
            button.setAttribute('aria-pressed', String(!showing));
            button.setAttribute('aria-label', showing ? 'Hiện mật khẩu' : 'Ẩn mật khẩu');
            button.querySelector('i').className =
                showing ? 'fa-regular fa-eye' : 'fa-regular fa-eye-slash';
        });
    });
</script>
</body>
</html>
