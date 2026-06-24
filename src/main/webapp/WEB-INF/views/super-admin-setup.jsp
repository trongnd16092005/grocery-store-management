<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Khởi tạo Super Admin</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body{min-height:100vh;display:grid;place-items:center;background:#eef2ff}
        .setup-card{width:min(540px,94vw);border:0;border-radius:18px;box-shadow:0 18px 50px rgba(15,23,42,.14)}
    </style>
</head>
<body>
<div class="card setup-card">
    <div class="card-body p-4 p-md-5">
        <div class="text-center mb-4">
            <div class="fs-1">🛡️</div>
            <h1 class="h4 fw-bold">Khởi tạo Super Admin</h1>
            <p class="text-muted mb-0">Thao tác này chỉ thực hiện một lần cho toàn hệ thống.</p>
        </div>
        <c:if test="${not empty error}"><div class="alert alert-danger"><c:out value="${error}"/></div></c:if>
        <form method="post">
            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
            <div class="mb-3">
                <label class="form-label fw-semibold">Khóa khởi tạo</label>
                <input type="password" class="form-control" name="setupKey" required
                       autocomplete="off">
                <div class="form-text">Giá trị đã cấu hình trong SUPER_ADMIN_SETUP_KEY.</div>
            </div>
            <div class="mb-3">
                <label class="form-label fw-semibold">Họ tên</label>
                <input class="form-control" name="fullName" required maxlength="120">
            </div>
            <div class="mb-3">
                <label class="form-label fw-semibold">Tên đăng nhập</label>
                <input class="form-control" name="username" required
                       pattern="[A-Za-z0-9._-]{3,50}" autocomplete="username">
            </div>
            <div class="row g-3">
                <div class="col-md-6">
                    <label class="form-label fw-semibold">Mật khẩu</label>
                    <input type="password" class="form-control" name="password"
                           minlength="10" required autocomplete="new-password">
                </div>
                <div class="col-md-6">
                    <label class="form-label fw-semibold">Xác nhận</label>
                    <input type="password" class="form-control" name="confirmPassword"
                           minlength="10" required autocomplete="new-password">
                </div>
            </div>
            <button class="btn btn-primary btn-lg w-100 mt-4">Tạo Super Admin</button>
        </form>
        <div class="text-center mt-3"><a href="${pageContext.request.contextPath}/login">Quay lại đăng nhập</a></div>
    </div>
</div>
</body>
</html>
