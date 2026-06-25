<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Phiếu nhập hàng</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=ui2">
</head>
<body>
<div class="container-fluid p-0 d-flex">
    <%@ include file="common/sidebar.jspf" %>
    <main class="main-content">
        <div class="topbar d-flex justify-content-between align-items-center">
            <div>
                <h1 class="h4 m-0 fw-bold"><i class="fa-solid fa-file-circle-plus me-2 text-primary"></i>Phiếu nhập hàng</h1>
                <small class="text-muted">Tạo nháp, kiểm tra rồi hoàn thành để cộng tồn</small>
            </div>
            <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#createModal">
                <i class="fa-solid fa-plus me-2"></i>Tạo phiếu nhập
            </button>
        </div>
        <div class="content">
            <c:if test="${not empty flashSuccess}"><div class="alert alert-success"><c:out value="${flashSuccess}"/></div></c:if>
            <c:if test="${not empty flashError}"><div class="alert alert-danger"><c:out value="${flashError}"/></div></c:if>

            <div class="card border-0 shadow-sm">
                <div class="card-body border-bottom">
                    <form method="get" class="row g-2">
                        <div class="col-md-7">
                            <input class="form-control" name="q" value="<c:out value='${keyword}'/>"
                                   placeholder="Tìm mã phiếu hoặc nhà cung cấp...">
                        </div>
                        <div class="col-md-3">
                            <select class="form-select" name="status">
                                <option value="">Tất cả trạng thái</option>
                                <option value="DRAFT" ${selectedStatus=='DRAFT'?'selected':''}>Nháp</option>
                                <option value="COMPLETED" ${selectedStatus=='COMPLETED'?'selected':''}>Hoàn thành</option>
                                <option value="CANCELLED" ${selectedStatus=='CANCELLED'?'selected':''}>Đã hủy</option>
                            </select>
                        </div>
                        <div class="col-md-2"><button class="btn btn-outline-primary w-100">Lọc</button></div>
                    </form>
                </div>
                <div class="table-responsive">
                    <table class="table table-hover align-middle mb-0">
                        <thead class="table-light">
                        <tr><th class="ps-4">Mã phiếu</th><th>Nhà cung cấp</th><th>Người tạo</th><th>Tổng tiền</th><th>Trạng thái</th><th>Ngày tạo</th><th></th></tr>
                        </thead>
                        <tbody>
                        <c:forEach items="${orders}" var="order">
                            <tr>
                                <td class="ps-4 fw-semibold"><c:out value="${order.code}"/></td>
                                <td><c:out value="${order.supplierName}"/></td>
                                <td><c:out value="${order.createdByName}" default="—"/></td>
                                <td class="fw-semibold"><fmt:formatNumber value="${order.totalAmount}" pattern="#,##0"/> đ</td>
                                <td>
                                    <span class="badge ${order.status=='DRAFT'?'bg-warning text-dark':order.status=='COMPLETED'?'bg-success':'bg-secondary'}">
                                        ${order.status=='DRAFT'?'Nháp':order.status=='COMPLETED'?'Hoàn thành':'Đã hủy'}
                                    </span>
                                </td>
                                <td class="small text-muted">${order.createdAt.toLocalDateTime()}</td>
                                <td class="text-end pe-4"><a class="btn btn-sm btn-outline-primary" href="?id=${order.id}">Chi tiết</a></td>
                            </tr>
                        </c:forEach>
                        <c:if test="${empty orders}"><tr><td colspan="7" class="text-center text-muted py-5">Chưa có phiếu nhập.</td></tr></c:if>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </main>
</div>

<div class="modal fade" id="createModal" tabindex="-1">
    <div class="modal-dialog modal-xl modal-dialog-centered">
        <div class="modal-content">
            <form method="post" action="${pageContext.request.contextPath}/purchase-orders">
                <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                <input type="hidden" name="action" value="create">
                <div class="modal-header"><h5 class="modal-title fw-bold">Tạo phiếu nhập nháp</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
                <div class="modal-body">
                    <div class="row g-3 mb-3">
                        <div class="col-md-6">
                            <label class="form-label fw-semibold">Nhà cung cấp</label>
                            <select class="form-select" name="supplierId" required>
                                <option value="">Chọn nhà cung cấp</option>
                                <c:forEach items="${suppliers}" var="supplier"><option value="${supplier.id}">${supplier.code} — <c:out value="${supplier.name}"/></option></c:forEach>
                            </select>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label fw-semibold">Ghi chú</label>
                            <input class="form-control" name="note" maxlength="500">
                        </div>
                    </div>
                    <div class="table-responsive">
                        <table class="table align-middle">
                            <thead class="table-light"><tr><th>Sản phẩm</th><th style="width:150px">Số lượng</th><th style="width:200px">Giá nhập</th><th style="width:60px"></th></tr></thead>
                            <tbody id="detailRows">
                            <tr class="detail-row">
                                <td><select class="form-select" name="productId" required><option value="">Chọn sản phẩm</option><c:forEach items="${products}" var="product"><option value="${product.id}">${product.code} — <c:out value="${product.name}"/></option></c:forEach></select></td>
                                <td><input type="number" class="form-control" name="quantity" min="1" required></td>
                                <td><input type="number" class="form-control" name="unitCost" min="0" step="0.01" required></td>
                                <td><button type="button" class="btn btn-outline-danger remove-row" title="Xóa dòng"><i class="fa-solid fa-xmark"></i></button></td>
                            </tr>
                            </tbody>
                        </table>
                    </div>
                    <button type="button" class="btn btn-sm btn-outline-primary" id="addRow"><i class="fa-solid fa-plus me-1"></i>Thêm sản phẩm</button>
                </div>
                <div class="modal-footer"><button type="button" class="btn btn-light" data-bs-dismiss="modal">Đóng</button><button class="btn btn-primary">Lưu phiếu nháp</button></div>
            </form>
        </div>
    </div>
</div>

<c:if test="${not empty selectedOrder}">
<div class="modal fade" id="detailModal" tabindex="-1">
    <div class="modal-dialog modal-lg modal-dialog-centered">
        <div class="modal-content">
            <div class="modal-header">
                <div><h5 class="modal-title">Phiếu nhập ${selectedOrder.code}</h5><small class="text-muted"><c:out value="${selectedOrder.supplierName}"/></small></div>
                <a class="btn-close" href="${pageContext.request.contextPath}/purchase-orders"></a>
            </div>
            <div class="modal-body">
                <div class="table-responsive"><table class="table"><thead><tr><th>Sản phẩm</th><th class="text-center">SL</th><th class="text-end">Giá nhập</th><th class="text-end">Thành tiền</th></tr></thead><tbody>
                <c:forEach items="${selectedOrder.details}" var="detail"><tr><td>${detail.productCode} — <c:out value="${detail.productName}"/></td><td class="text-center">${detail.quantity}</td><td class="text-end"><fmt:formatNumber value="${detail.unitCost}" pattern="#,##0"/> đ</td><td class="text-end"><fmt:formatNumber value="${detail.lineTotal}" pattern="#,##0"/> đ</td></tr></c:forEach>
                </tbody><tfoot><tr><th colspan="3" class="text-end">Tổng cộng</th><th class="text-end text-danger"><fmt:formatNumber value="${selectedOrder.totalAmount}" pattern="#,##0"/> đ</th></tr></tfoot></table></div>
                <c:if test="${not empty selectedOrder.note}"><div class="alert alert-light border mb-0"><strong>Ghi chú:</strong> <c:out value="${selectedOrder.note}"/></div></c:if>
            </div>
            <div class="modal-footer">
                <a class="btn btn-light" href="${pageContext.request.contextPath}/purchase-orders">Đóng</a>
                <c:if test="${selectedOrder.status=='DRAFT'}">
                    <form method="post" action="${pageContext.request.contextPath}/purchase-orders" class="d-inline" onsubmit="return confirm('Hoàn thành phiếu và cộng toàn bộ sản phẩm vào tồn kho?')"><input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="complete"><input type="hidden" name="id" value="${selectedOrder.id}"><button class="btn btn-success">Hoàn thành & cộng tồn</button></form>
                </c:if>
                <c:if test="${selectedOrder.status!='CANCELLED'}">
                    <form method="post" action="${pageContext.request.contextPath}/purchase-orders" class="d-inline" onsubmit="return confirm('Hủy phiếu nhập? Nếu phiếu đã hoàn thành, tồn kho sẽ bị trừ lại.')"><input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"><input type="hidden" name="action" value="cancel"><input type="hidden" name="id" value="${selectedOrder.id}"><button class="btn btn-danger">Hủy phiếu</button></form>
                </c:if>
            </div>
        </div>
    </div>
</div>
</c:if>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>

const rows=document.getElementById('detailRows');
document.getElementById('addRow').addEventListener('click',()=>{
    const clone=rows.querySelector('.detail-row').cloneNode(true);
    clone.querySelectorAll('input,select').forEach(input=>input.value='');
    rows.appendChild(clone);
});
rows.addEventListener('click',event=>{
    const button=event.target.closest('.remove-row');
    if(!button)return;
    if(rows.querySelectorAll('.detail-row').length===1){alert('Phiếu nhập cần ít nhất một sản phẩm.');return;}
    button.closest('.detail-row').remove();
});
<c:if test="${not empty selectedOrder}">bootstrap.Modal.getOrCreateInstance(document.getElementById('detailModal')).show();</c:if>
</script>
</body>
</html>
