<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Quản trị hệ thống</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
    <style>.metric{border:0;border-radius:14px;box-shadow:0 4px 18px rgba(15,23,42,.06)}</style>
</head>
<body>
<div class="container-fluid p-0 d-flex">
    <div id="sidebar-placeholder"></div>
    <main class="main-content">
        <div class="topbar d-flex justify-content-between align-items-center">
            <div><h1 class="h4 m-0 fw-bold">🛡️ Quản Trị Hệ Thống</h1><small class="text-muted">Quản lý các cửa hàng sử dụng nền tảng</small></div>
            <div class="text-end"><strong><c:out value="${sessionScope.currentUser.fullName}"/></strong><div class="small text-primary">Super Admin</div></div>
        </div>
        <div class="content">
            <c:if test="${not empty flashSuccess}"><div class="alert alert-success"><c:out value="${flashSuccess}"/></div></c:if>
            <c:if test="${not empty flashError}"><div class="alert alert-danger"><c:out value="${flashError}"/></div></c:if>
            <div class="row g-3 mb-4">
                <div class="col-md-4"><div class="card metric"><div class="card-body"><small class="text-muted">Tổng cửa hàng</small><div class="fs-2 fw-bold">${totalStores}</div></div></div></div>
                <div class="col-md-4"><div class="card metric"><div class="card-body"><small class="text-muted">Đang hoạt động</small><div class="fs-2 fw-bold text-success">${activeStores}</div></div></div></div>
                <div class="col-md-4"><div class="card metric"><div class="card-body"><small class="text-muted">Đã khóa</small><div class="fs-2 fw-bold text-danger">${inactiveStores}</div></div></div></div>
            </div>
            <div class="card border-0 shadow-sm">
                <div class="card-header bg-white py-3"><h2 class="h6 fw-bold mb-0">Danh sách cửa hàng</h2></div>
                <div class="table-responsive">
                    <table class="table table-hover align-middle mb-0">
                        <thead class="table-light"><tr><th class="ps-4">Mã</th><th>Cửa hàng</th><th>Liên hệ</th><th class="text-center">ADMIN</th><th class="text-center">Nhân viên</th><th>Ngày đăng ký</th><th>Trạng thái</th><th class="text-end pe-4"></th></tr></thead>
                        <tbody>
                        <c:forEach items="${stores}" var="store">
                            <tr class="${store.active?'':'opacity-75'}">
                                <td class="ps-4 fw-semibold"><c:out value="${store.code}"/></td>
                                <td><div class="fw-semibold"><c:out value="${store.name}"/></div><small class="text-muted"><c:out value="${store.address}"/></small></td>
                                <td><c:out value="${store.phone}" default="—"/></td>
                                <td class="text-center"><span class="badge bg-primary rounded-pill">${store.adminCount}</span></td>
                                <td class="text-center"><span class="badge bg-info text-dark rounded-pill">${store.employeeCount}</span></td>
                                <td class="small text-muted">${store.createdAt.toLocalDateTime()}</td>
                                <td><span class="badge ${store.active?'bg-success':'bg-secondary'}">${store.active?'Hoạt động':'Đã khóa'}</span></td>
                                <td class="text-end pe-4">
                                    <form method="post" action="${pageContext.request.contextPath}/super-admin"
                                          onsubmit="return confirm('${store.active?'Khóa cửa hàng này? Tất cả phiên đăng nhập sẽ bị ngắt.':'Mở khóa cửa hàng này?'}')">
                                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="id" value="${store.id}">
                                        <input type="hidden" name="action" value="${store.active?'lock':'unlock'}">
                                        <button class="btn btn-sm ${store.active?'btn-outline-danger':'btn-outline-success'}">
                                            <i class="fa-solid ${store.active?'fa-lock':'fa-lock-open'} me-1"></i>${store.active?'Khóa':'Mở khóa'}
                                        </button>
                                    </form>
                                </td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty stores}"><tr><td colspan="8" class="text-center text-muted py-5">Chưa có cửa hàng đăng ký.</td></tr></c:if>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </main>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>fetch('${pageContext.request.contextPath}/common/sidebar').then(r=>r.text()).then(html=>{document.getElementById('sidebar-placeholder').outerHTML=html;document.querySelector('.sidebar-nav a[href$="/super-admin"]')?.classList.add('active');});</script>
</body>
</html>
