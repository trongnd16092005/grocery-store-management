<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Thông tin cửa hàng</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
</head>
<body>
<div class="container-fluid p-0 d-flex">
    <div id="sidebar-placeholder"></div>
    <main class="main-content">
        <div class="topbar">
            <h1 class="h4 m-0 fw-bold"><i class="fa-solid fa-store me-2"></i>Thông Tin Cửa Hàng</h1>
        </div>
        <div class="content">
            <c:if test="${not empty flashSuccess}">
                <div class="alert alert-success"><c:out value="${flashSuccess}"/></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="alert alert-danger"><c:out value="${flashError}"/></div>
            </c:if>

            <div class="row justify-content-center">
                <div class="col-xl-8">
                    <div class="card border-0 shadow-sm">
                        <div class="card-body p-4 p-lg-5">
                            <div class="d-flex align-items-center gap-3 mb-4">
                                <div class="rounded-3 bg-primary-subtle text-primary p-3 fs-3">
                                    <i class="fa-solid fa-shop"></i>
                                </div>
                                <div>
                                    <h2 class="h5 fw-bold mb-1"><c:out value="${store.name}"/></h2>
                                    <div class="text-muted">Mã cửa hàng: <strong><c:out value="${store.code}"/></strong></div>
                                </div>
                            </div>

                            <form method="post" action="${pageContext.request.contextPath}/store">
                                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                <div class="mb-3">
                                    <label class="form-label fw-semibold">Mã cửa hàng</label>
                                    <input class="form-control bg-light" value="<c:out value='${store.code}'/>" disabled>
                                    <div class="form-text">Mã dùng khi nhân viên đăng nhập và không thể thay đổi.</div>
                                </div>
                                <div class="mb-3">
                                    <label class="form-label fw-semibold">Tên cửa hàng <span class="text-danger">*</span></label>
                                    <input class="form-control" name="name" maxlength="150" required
                                           value="<c:out value='${store.name}'/>">
                                </div>
                                <div class="mb-3">
                                    <label class="form-label fw-semibold">Số điện thoại</label>
                                    <input class="form-control" name="phone" inputmode="numeric" maxlength="20"
                                           value="<c:out value='${store.phone}'/>" placeholder="Ví dụ: 0901234567">
                                </div>
                                <div class="mb-4">
                                    <label class="form-label fw-semibold">Địa chỉ</label>
                                    <textarea class="form-control" name="address" rows="4" maxlength="500"
                                              placeholder="Địa chỉ cửa hàng"><c:out value="${store.address}"/></textarea>
                                </div>
                                <button class="btn btn-primary">
                                    <i class="fa-solid fa-floppy-disk me-2"></i>Lưu thông tin
                                </button>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </main>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
fetch('${pageContext.request.contextPath}/common/sidebar').then(r => r.text()).then(html => {
    document.getElementById('sidebar-placeholder').outerHTML = html;
    document.querySelector('.sidebar-nav a[href$="/store"]')?.classList.add('active');
});
</script>
</body>
</html>
