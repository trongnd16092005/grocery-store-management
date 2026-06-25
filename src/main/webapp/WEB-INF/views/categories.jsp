<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản lý danh mục</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=ui2">
</head>
<body>
<div class="container-fluid p-0 d-flex">
    <%@ include file="common/sidebar.jspf" %>
    <main class="main-content">
        <div class="topbar"><h1 class="h4 m-0 fw-bold"><i class="fa-solid fa-tags me-2 text-primary"></i>Quản lý danh mục</h1></div>
        <div class="content">
            <c:if test="${not empty flashSuccess}"><div class="alert alert-success"><c:out value="${flashSuccess}"/></div></c:if>
            <c:if test="${not empty flashError}"><div class="alert alert-danger"><c:out value="${flashError}"/></div></c:if>

            <div class="row g-4">
                <div class="col-lg-4">
                    <div class="card border-0 shadow-sm">
                        <div class="card-body p-4">
                            <h5 class="fw-bold mb-3">${empty editingCategory ? 'Thêm danh mục' : 'Sửa danh mục'}</h5>
                            <form method="post" action="${pageContext.request.contextPath}/categories"><input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                <input type="hidden" name="action" value="save">
                                <input type="hidden" name="id" value="${editingCategory.id}">
                                <div class="mb-3">
                                    <label class="form-label fw-semibold">Tên danh mục</label>
                                    <input class="form-control" name="name" maxlength="100" required
                                           value="<c:out value='${editingCategory.name}'/>">
                                </div>
                                <div class="mb-3">
                                    <label class="form-label fw-semibold">Mô tả</label>
                                    <textarea class="form-control" name="description" rows="4" maxlength="500"><c:out value="${editingCategory.description}"/></textarea>
                                </div>
                                <button class="btn btn-primary w-100"><i class="fa-solid fa-floppy-disk me-2"></i>Lưu danh mục</button>
                                <c:if test="${not empty editingCategory}">
                                    <a class="btn btn-light border w-100 mt-2" href="${pageContext.request.contextPath}/categories">Hủy sửa</a>
                                </c:if>
                            </form>
                        </div>
                    </div>
                </div>

                <div class="col-lg-8">
                    <div class="card border-0 shadow-sm">
                        <div class="card-header bg-white border-0 p-3">
                            <form method="get" class="d-flex gap-2">
                                <input class="form-control" name="q" value="<c:out value='${keyword}'/>" placeholder="Tìm mã, tên hoặc mô tả...">
                                <button class="btn btn-outline-primary"><i class="fa-solid fa-magnifying-glass"></i></button>
                                <a class="btn btn-light border" href="${pageContext.request.contextPath}/categories">Xóa lọc</a>
                            </form>
                        </div>
                        <div class="table-responsive">
                            <table class="table table-hover align-middle mb-0">
                                <thead class="table-light"><tr><th class="ps-4">Mã DM</th><th>Tên danh mục</th><th>Mô tả</th><th class="text-center">Số SP</th><th class="text-end pe-4">Thao tác</th></tr></thead>
                                <tbody>
                                <c:forEach items="${categories}" var="category">
                                    <tr>
                                        <td class="ps-4 text-muted"><c:out value="${category.code}"/></td>
                                        <td class="fw-semibold"><c:out value="${category.name}"/></td>
                                        <td class="text-muted"><c:out value="${category.description}"/></td>
                                        <td class="text-center"><span class="badge bg-primary rounded-pill">${category.productCount}</span></td>
                                        <td class="text-end pe-4">
                                            <a class="btn btn-sm btn-outline-primary" href="${pageContext.request.contextPath}/categories?editId=${category.id}"><i class="fa-solid fa-pen"></i></a>
                                            <form method="post" action="${pageContext.request.contextPath}/categories" class="d-inline" onsubmit="return confirm('Xóa danh mục này?')"><input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                                <input type="hidden" name="action" value="delete"><input type="hidden" name="id" value="${category.id}">
                                                <button class="btn btn-sm btn-outline-danger"><i class="fa-solid fa-trash"></i></button>
                                            </form>
                                        </td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty categories}"><tr><td colspan="5" class="text-center text-muted py-5">Không tìm thấy danh mục.</td></tr></c:if>
                                </tbody>
                            </table>
                        </div>
                        <div class="card-footer bg-white border-0 small text-muted">Hiển thị ${categories.size()} danh mục</div>
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
