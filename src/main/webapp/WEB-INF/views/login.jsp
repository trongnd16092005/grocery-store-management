<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Đăng nhập | Quản lý cửa hàng</title>
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
<main class="login-page">
    <section class="login-intro" aria-label="Giới thiệu hệ thống">
        <div class="brand">
            <span class="brand-mark" aria-hidden="true">
                <i class="fa-solid fa-store"></i>
            </span>
            <span>Retail Store</span>
        </div>

        <div class="intro-content">
            <p class="eyebrow">Hệ thống quản lý bán lẻ</p>
            <h1>Vận hành cửa hàng<br>trên một màn hình.</h1>
            <p class="intro-description">
                Theo dõi bán hàng, tồn kho, khách hàng và nhập hàng
                trong cùng một hệ thống.
            </p>
        </div>

        <p class="intro-note">
            Dữ liệu của mỗi cửa hàng được quản lý độc lập.
        </p>
    </section>

    <section class="login-panel">
        <div class="login-form-wrap">
            <header class="login-header">
                <div class="mobile-brand">
                    <span class="brand-mark" aria-hidden="true">
                        <i class="fa-solid fa-store"></i>
                    </span>
                    <span>Retail Store</span>
                </div>
                <h2>Đăng nhập</h2>
                <p>Nhập thông tin tài khoản của cửa hàng để tiếp tục.</p>
            </header>

            <c:if test="${not empty info}">
                <div class="alert alert-info login-alert" role="status">
                    <i class="fa-solid fa-circle-info" aria-hidden="true"></i>
                    <c:out value="${info}"/>
                </div>
            </c:if>
            <c:if test="${not empty error}">
                <div class="alert alert-danger login-alert" role="alert">
                    <i class="fa-solid fa-circle-exclamation" aria-hidden="true"></i>
                    <c:out value="${error}"/>
                </div>
            </c:if>

            <form method="post" class="login-form">
                <input
                        type="hidden"
                        name="csrfToken"
                        value="${sessionScope.csrfToken}">

                <div class="form-field">
                    <label for="storeCode">Mã cửa hàng</label>
                    <div class="input-wrap">
                        <i class="fa-solid fa-building" aria-hidden="true"></i>
                        <input
                                id="storeCode"
                                class="form-control text-uppercase"
                                name="storeCode"
                                value="<c:out value='${storeCode}'/>"
                                placeholder="Ví dụ: CUAHANGABC"
                                required
                                autocomplete="organization">
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
                                value="<c:out value='${username}'/>"
                                placeholder="Nhập tên đăng nhập"
                                required
                                autofocus
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
                                placeholder="Nhập mật khẩu"
                                required
                                autocomplete="current-password">
                        <button
                                class="password-toggle"
                                type="button"
                                aria-label="Hiện mật khẩu"
                                aria-pressed="false">
                            <i class="fa-regular fa-eye" aria-hidden="true"></i>
                        </button>
                    </div>
                </div>

                <button class="btn-login" type="submit">
                    Đăng nhập
                    <i class="fa-solid fa-arrow-right" aria-hidden="true"></i>
                </button>
            </form>

            <div class="register-link">
                Chưa có cửa hàng?
                <a href="${pageContext.request.contextPath}/register">
                    Tạo cửa hàng mới
                </a>
            </div>
        </div>
    </section>
</main>

<script>
    const passwordInput = document.getElementById('password');
    const passwordToggle = document.querySelector('.password-toggle');

    passwordToggle.addEventListener('click', () => {
        const showing = passwordInput.type === 'text';
        passwordInput.type = showing ? 'password' : 'text';
        passwordToggle.setAttribute('aria-pressed', String(!showing));
        passwordToggle.setAttribute(
            'aria-label',
            showing ? 'Hiện mật khẩu' : 'Ẩn mật khẩu'
        );
        passwordToggle.querySelector('i').className =
            showing ? 'fa-regular fa-eye' : 'fa-regular fa-eye-slash';
    });
</script>
</body>
</html>
