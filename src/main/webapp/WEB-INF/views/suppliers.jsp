<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản lý nhà cung cấp</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=ui2">
</head>
<body>
<div class="container-fluid p-0 d-flex">
    <%@ include file="common/sidebar.jspf" %>
    <main class="main-content">
        <div class="topbar">
            <h1 class="h4 m-0 fw-bold"><i class="fa-solid fa-truck-field me-2 text-primary"></i>Quản lý nhà cung cấp</h1>
        </div>
        <div class="content">
            <c:if test="${not empty flashSuccess}">
                <div class="alert alert-success"><c:out value="${flashSuccess}"/></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="alert alert-danger"><c:out value="${flashError}"/></div>
            </c:if>

            <div class="row g-4">
                <div class="col-xl-4">
                    <div class="card border-0 shadow-sm">
                        <div class="card-body p-4">
                            <h2 class="h5 fw-bold mb-3">
                                ${empty editingSupplier ? 'Thêm nhà cung cấp' : 'Sửa nhà cung cấp'}
                            </h2>
                            <form method="post" action="${pageContext.request.contextPath}/suppliers">
                                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="save">
                                <input type="hidden" name="id" value="${editingSupplier.id}">
                                <div class="mb-3">
                                    <label class="form-label fw-semibold">Tên nhà cung cấp <span class="text-danger">*</span></label>
                                    <input class="form-control" name="name" maxlength="150" required
                                           value="<c:out value='${editingSupplier.name}'/>">
                                </div>
                                <div class="mb-3">
                                    <label class="form-label fw-semibold">Số điện thoại</label>
                                    <input class="form-control" name="phone" inputmode="numeric" maxlength="20"
                                           value="<c:out value='${editingSupplier.phone}'/>">
                                </div>
                                <div class="mb-3">
                                    <label class="form-label fw-semibold">Email</label>
                                    <input class="form-control" name="email" type="email" maxlength="150"
                                           value="<c:out value='${editingSupplier.email}'/>">
                                </div>
                                <div class="mb-3">
                                    <label class="form-label fw-semibold">Địa chỉ</label>
                                    <textarea class="form-control" name="address" rows="3" maxlength="500"><c:out value="${editingSupplier.address}"/></textarea>
                                </div>
                                <button class="btn btn-primary w-100">
                                    <i class="fa-solid fa-floppy-disk me-2"></i>Lưu nhà cung cấp
                                </button>
                                <c:if test="${not empty editingSupplier}">
                                    <a class="btn btn-light border w-100 mt-2"
                                       href="${pageContext.request.contextPath}/suppliers">Hủy sửa</a>
                                </c:if>
                            </form>
                        </div>
                    </div>
                </div>

                <div class="col-xl-8">
                    <div class="card border-0 shadow-sm">
                        <div class="card-header bg-white border-0 p-3">
                            <form method="get" class="row g-2 align-items-center">
                                <div class="col">
                                    <input class="form-control" name="q" value="<c:out value='${keyword}'/>"
                                           placeholder="Tìm mã, tên, điện thoại hoặc email...">
                                </div>
                                <div class="col-auto">
                                    <div class="form-check">
                                        <input class="form-check-input" type="checkbox" name="includeInactive"
                                               value="true" id="includeInactive" ${includeInactive ? 'checked' : ''}>
                                        <label class="form-check-label" for="includeInactive">Hiện đã khóa</label>
                                    </div>
                                </div>
                                <div class="col-auto">
                                    <button class="btn btn-outline-primary" title="Tìm kiếm">
                                        <i class="fa-solid fa-magnifying-glass"></i>
                                    </button>
                                    <a class="btn btn-light border" href="${pageContext.request.contextPath}/suppliers">Xóa lọc</a>
                                </div>
                            </form>
                        </div>
                        <div class="table-responsive">
                            <table class="table table-hover align-middle mb-0">
                                <thead class="table-light">
                                <tr>
                                    <th class="ps-4">Mã NCC</th>
                                    <th>Nhà cung cấp</th>
                                    <th>Liên hệ</th>
                                    <th class="text-center">Sản phẩm</th>
                                    <th>Trạng thái</th>
                                    <th class="text-end pe-4">Thao tác</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach items="${suppliers}" var="supplier">
                                    <tr class="${supplier.active ? '' : 'opacity-75'}">
                                        <td class="ps-4 text-muted"><c:out value="${supplier.code}"/></td>
                                        <td>
                                            <div class="fw-semibold"><c:out value="${supplier.name}"/></div>
                                            <small class="text-muted"><c:out value="${supplier.address}"/></small>
                                        </td>
                                        <td>
                                            <div><c:out value="${supplier.phone}"/></div>
                                            <small class="text-muted"><c:out value="${supplier.email}"/></small>
                                        </td>
                                        <td class="text-center">
                                            <span class="badge bg-primary rounded-pill">${supplier.productCount}</span>
                                        </td>
                                        <td>
                                            <span class="badge ${supplier.active ? 'bg-success-subtle text-success' : 'bg-secondary-subtle text-secondary'}">
                                                ${supplier.active ? 'Đang hoạt động' : 'Đã khóa'}
                                            </span>
                                        </td>
                                        <td class="text-end pe-4 text-nowrap">
                                            <a class="btn btn-sm btn-outline-primary"
                                               href="${pageContext.request.contextPath}/suppliers?editId=${supplier.id}"
                                               title="Chỉnh sửa"><i class="fa-solid fa-pen"></i></a>
                                            <form method="post" action="${pageContext.request.contextPath}/suppliers"
                                                  class="d-inline"
                                                  onsubmit="return confirm('${supplier.active ? 'Khóa' : 'Mở khóa'} nhà cung cấp này?')">
                                                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                                <input type="hidden" name="action" value="${supplier.active ? 'lock' : 'unlock'}">
                                                <input type="hidden" name="id" value="${supplier.id}">
                                                <button class="btn btn-sm ${supplier.active ? 'btn-outline-danger' : 'btn-outline-success'}"
                                                        title="${supplier.active ? 'Khóa' : 'Mở khóa'}">
                                                    <i class="fa-solid ${supplier.active ? 'fa-lock' : 'fa-lock-open'}"></i>
                                                </button>
                                            </form>
                                        </td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty suppliers}">
                                    <tr><td colspan="6" class="text-center text-muted py-5">Không tìm thấy nhà cung cấp.</td></tr>
                                </c:if>
                                </tbody>
                            </table>
                        </div>
                        <div class="card-footer bg-white border-0 small text-muted">
                            Hiển thị ${suppliers.size()} nhà cung cấp
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </main>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>

</script>
</body>
</html>
