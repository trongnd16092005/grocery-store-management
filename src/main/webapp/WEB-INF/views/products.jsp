<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Quản lý sản phẩm</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=ui2">
</head>
<body>
<div class="container-fluid p-0 d-flex"><%@ include file="common/sidebar.jspf" %>
<main class="main-content">
    <div class="topbar d-flex justify-content-between align-items-center">
        <div><h1 class="h4 m-0 fw-bold"><i class="fa-solid fa-box-open me-2 text-primary"></i>Quản lý sản phẩm</h1><small class="text-muted">Tồn kho chỉ thay đổi tại màn hình Kho</small></div>
        <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#productModal"><i class="fa-solid fa-plus me-2"></i>Thêm sản phẩm</button>
    </div>
    <div class="content">
        <c:if test="${not empty flashSuccess}"><div class="alert alert-success"><c:out value="${flashSuccess}"/></div></c:if>
        <c:if test="${not empty flashError}"><div class="alert alert-danger"><c:out value="${flashError}"/></div></c:if>

        <div class="card border-0 shadow-sm mb-4"><div class="card-body">
            <form method="get" class="row g-2">
                <div class="col-lg-5"><input class="form-control" name="q" value="<c:out value='${keyword}'/>" placeholder="Tìm mã, tên hoặc mã vạch..."></div>
                <div class="col-sm-6 col-lg-3"><select class="form-select" name="categoryId"><option value="">Tất cả danh mục</option><c:forEach items="${categories}" var="category"><option value="${category.id}" ${selectedCategoryId == category.id ? 'selected' : ''}><c:out value="${category.name}"/></option></c:forEach></select></div>
                <div class="col-sm-6 col-lg-2"><select class="form-select" name="stockStatus"><option value="">Tất cả tồn kho</option><option value="OK" ${stockStatus == 'OK' ? 'selected' : ''}>Đủ hàng</option><option value="LOW" ${stockStatus == 'LOW' ? 'selected' : ''}>Sắp hết</option><option value="OUT" ${stockStatus == 'OUT' ? 'selected' : ''}>Hết hàng</option></select></div>
                <div class="col-lg-2 d-flex gap-2"><button class="btn btn-outline-primary flex-fill">Lọc</button><a class="btn btn-light border" href="${pageContext.request.contextPath}/products"><i class="fa-solid fa-rotate-left"></i></a></div>
            </form>
        </div></div>

        <div class="card border-0 shadow-sm"><div class="table-responsive">
            <table class="table table-hover align-middle mb-0"><thead class="table-light"><tr><th class="ps-4">Sản phẩm</th><th>Danh mục</th><th>Giá nhập</th><th>Giá bán</th><th>Tồn</th><th>Trạng thái</th><th class="text-end pe-4">Thao tác</th></tr></thead><tbody>
            <c:forEach items="${products}" var="product"><tr>
                <td class="ps-4"><div class="fw-semibold"><c:out value="${product.name}"/></div><small class="text-muted">${product.code}<c:if test="${not empty product.barcode}"> · ${product.barcode}</c:if></small></td>
                <td><c:out value="${product.categoryName}"/></td>
                <td><fmt:formatNumber value="${product.costPrice}" pattern="#,#00"/> đ</td>
                <td class="fw-semibold"><fmt:formatNumber value="${product.sellingPrice}" pattern="#,#00"/> đ</td>
                <td>${product.stockQuantity} ${product.unit}</td>
                <td><c:choose><c:when test="${product.outOfStock}"><span class="badge bg-danger">Hết hàng</span></c:when><c:when test="${product.lowStock}"><span class="badge bg-warning text-dark">Sắp hết</span></c:when><c:otherwise><span class="badge bg-success">Đủ hàng</span></c:otherwise></c:choose></td>
                <td class="text-end pe-4"><a class="btn btn-sm btn-outline-primary" href="${pageContext.request.contextPath}/products?editId=${product.id}"><i class="fa-solid fa-pen"></i></a>
                    <form method="post" class="d-inline" action="${pageContext.request.contextPath}/products" onsubmit="return confirm('Ngừng kinh doanh sản phẩm này?')"><input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="delete"><input type="hidden" name="id" value="${product.id}"><button class="btn btn-sm btn-outline-danger"><i class="fa-solid fa-ban"></i></button></form></td>
            </tr></c:forEach>
            <c:if test="${empty products}"><tr><td colspan="7" class="text-center text-muted py-5">Không tìm thấy sản phẩm.</td></tr></c:if>
            </tbody></table>
        </div>
        <div class="card-footer bg-white border-0 d-flex justify-content-between align-items-center"><small class="text-muted">Tổng ${total} sản phẩm</small><nav><ul class="pagination pagination-sm mb-0"><c:forEach begin="1" end="${totalPages}" var="number"><li class="page-item ${number == page ? 'active' : ''}"><a class="page-link" href="${pageContext.request.contextPath}/products?page=${number}&q=${keyword}&categoryId=${selectedCategoryId}&stockStatus=${stockStatus}">${number}</a></li></c:forEach></ul></nav></div>
        </div>
    </div>
</main></div>

<div class="modal fade" id="productModal" tabindex="-1"><div class="modal-dialog modal-lg modal-dialog-centered"><div class="modal-content"><form method="post" action="${pageContext.request.contextPath}/products"><input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
    <div class="modal-header"><div><h5 class="modal-title fw-bold">${empty editingProduct ? 'Thêm sản phẩm' : 'Sửa sản phẩm'}</h5><small class="text-muted">Tồn đầu kỳ của sản phẩm mới là 0</small></div><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
    <div class="modal-body"><input type="hidden" name="action" value="save"><input type="hidden" name="id" value="${editingProduct.id}">
        <div class="row g-3">
            <div class="col-md-8"><label class="form-label fw-semibold">Tên sản phẩm</label><input class="form-control" name="name" required maxlength="180" value="<c:out value='${editingProduct.name}'/>"></div>
            <div class="col-md-4"><label class="form-label fw-semibold">Mã vạch</label><input class="form-control" name="barcode" maxlength="50" value="<c:out value='${editingProduct.barcode}'/>"></div>
            <div class="col-md-6"><label class="form-label fw-semibold">Danh mục</label><select class="form-select" name="categoryId" required><option value="">Chọn danh mục</option><c:forEach items="${categories}" var="category"><option value="${category.id}" ${editingProduct.categoryId == category.id ? 'selected' : ''}><c:out value="${category.name}"/></option></c:forEach></select></div>
            <div class="col-md-6"><label class="form-label fw-semibold">Nhà cung cấp</label><select class="form-select" name="supplierId"><option value="">Chưa chọn</option><c:forEach items="${suppliers}" var="supplier"><option value="${supplier.id}" ${editingProduct.supplierId == supplier.id ? 'selected' : ''}><c:out value="${supplier.name}"/></option></c:forEach></select></div>
            <div class="col-md-4"><label class="form-label fw-semibold">Giá nhập</label><input type="number" min="0" class="form-control" name="costPrice" value="${empty editingProduct ? 0 : editingProduct.costPrice}" required></div>
            <div class="col-md-4"><label class="form-label fw-semibold">Giá bán</label><input type="number" min="1" class="form-control" name="sellingPrice" value="${editingProduct.sellingPrice}" required></div>
            <div class="col-md-2"><label class="form-label fw-semibold">Tồn tối thiểu</label><input type="number" min="0" class="form-control" name="minimumStock" value="${empty editingProduct ? 0 : editingProduct.minimumStock}" required></div>
            <div class="col-md-2"><label class="form-label fw-semibold">Đơn vị</label><input class="form-control" name="unit" value="${empty editingProduct ? 'cái' : editingProduct.unit}" required></div>
        </div>
    </div><div class="modal-footer"><a class="btn btn-light" href="${pageContext.request.contextPath}/products">Hủy</a><button class="btn btn-primary">Lưu sản phẩm</button></div>
</form></div></div></div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>

<c:if test="${not empty editingProduct}">bootstrap.Modal.getOrCreateInstance(document.getElementById('productModal')).show();</c:if>
</script>
</body></html>
