<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Đăng nhập</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body { min-height: 100vh; display: grid; place-items: center; background: linear-gradient(145deg,#eff6ff,#f8fafc); }
        .login-card { width: min(420px,92vw); border: 0; border-radius: 18px; box-shadow: 0 18px 50px rgba(15,23,42,.12); }
    </style>
</head>
<body>
<div class="card login-card">
    <div class="card-body p-4 p-md-5">
        <div class="text-center mb-4">
            <div class="fs-1">🏪</div>
            <h1 class="h4 fw-bold">Cửa hàng ABC</h1>
            <p class="text-muted mb-0">Đăng nhập để tiếp tục</p>
        </div>
        <c:if test="${not empty info}"><div class="alert alert-info"><c:out value="${info}"/></div></c:if>
        <c:if test="${not empty error}"><div class="alert alert-danger"><c:out value="${error}"/></div></c:if>
        <form method="post">
            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
            <div class="mb-3">
                <label class="form-label fw-semibold">Mã cửa hàng</label>
                <input class="form-control form-control-lg text-uppercase" name="storeCode"
                       value="<c:out value='${storeCode}'/>" required autocomplete="organization">
            </div>
            <div class="mb-3">
                <label class="form-label fw-semibold">Tên đăng nhập</label>
                <input class="form-control form-control-lg" name="username" required autofocus autocomplete="username">
            </div>
            <div class="mb-4">
                <label class="form-label fw-semibold">Mật khẩu</label>
                <input type="password" class="form-control form-control-lg" name="password" required autocomplete="current-password">
            </div>
            <button class="btn btn-primary btn-lg w-100">Đăng nhập</button>
        </form>
        <div class="text-center mt-3"><a href="${pageContext.request.contextPath}/register">Đăng ký cửa hàng mới</a></div>
        <div class="text-center mt-2 small"><a class="text-muted" href="${pageContext.request.contextPath}/super-admin/setup">Khởi tạo Super Admin lần đầu</a></div>
    </div>
</div>
</body>
</html>
