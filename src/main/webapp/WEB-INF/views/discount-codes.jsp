<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Quản lý mã giảm giá</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
</head>
<body>
<div class="container-fluid p-0 d-flex">
    <div id="sidebar-placeholder"></div>
    <main class="main-content">
        <div class="topbar"><h1 class="h4 m-0 fw-bold">🎟️ Quản Lý Mã Giảm Giá</h1></div>
        <div class="content">
            <c:if test="${not empty flashSuccess}"><div class="alert alert-success"><c:out value="${flashSuccess}"/></div></c:if>
            <c:if test="${not empty flashError}"><div class="alert alert-danger"><c:out value="${flashError}"/></div></c:if>
            <div class="row g-4">
                <div class="col-xl-4">
                    <div class="card border-0 shadow-sm"><div class="card-body p-4">
                        <h2 class="h5 fw-bold mb-3">${empty editingCode?'Tạo mã mới':'Sửa mã giảm giá'}</h2>
                        <form method="post" action="${pageContext.request.contextPath}/discount-codes">
                            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="action" value="save">
                            <input type="hidden" name="id" value="${editingCode.id}">
                            <div class="row g-3">
                                <div class="col-5"><label class="form-label fw-semibold">Mã</label><input class="form-control text-uppercase" name="code" maxlength="40" pattern="[A-Za-z0-9_-]{3,40}" required value="<c:out value='${editingCode.code}'/>"></div>
                                <div class="col-7"><label class="form-label fw-semibold">Tên chương trình</label><input class="form-control" name="name" maxlength="150" required value="<c:out value='${editingCode.name}'/>"></div>
                                <div class="col-5"><label class="form-label fw-semibold">Kiểu giảm</label><select class="form-select" name="discountType"><option value="PERCENT" ${editingCode.discountType=='PERCENT'?'selected':''}>Phần trăm (%)</option><option value="AMOUNT" ${editingCode.discountType=='AMOUNT'?'selected':''}>Số tiền (đ)</option></select></div>
                                <div class="col-7"><label class="form-label fw-semibold">Giá trị giảm</label><input type="number" min="0.01" step="0.01" class="form-control" name="discountValue" required value="${editingCode.discountValue}"></div>
                                <div class="col-6"><label class="form-label fw-semibold">Đơn tối thiểu</label><input type="number" min="0" step="1000" class="form-control" name="minimumOrder" value="${empty editingCode?0:editingCode.minimumOrder}"></div>
                                <div class="col-6"><label class="form-label fw-semibold">Giảm tối đa</label><input type="number" min="1" step="1000" class="form-control" name="maximumDiscount" value="${editingCode.maximumDiscount}" placeholder="Không giới hạn"></div>
                                <div class="col-6"><label class="form-label fw-semibold">Bắt đầu</label><input type="datetime-local" class="form-control" name="startsAt" value="${editingStartsAt}"></div>
                                <div class="col-6"><label class="form-label fw-semibold">Kết thúc</label><input type="datetime-local" class="form-control" name="endsAt" value="${editingEndsAt}"></div>
                                <div class="col-12"><label class="form-label fw-semibold">Giới hạn lượt dùng</label><input type="number" min="1" class="form-control" name="usageLimit" value="${editingCode.usageLimit}" placeholder="Để trống nếu không giới hạn"></div>
                            </div>
                            <button class="btn btn-primary w-100 mt-4"><i class="fa-solid fa-floppy-disk me-2"></i>Lưu mã</button>
                            <c:if test="${not empty editingCode}"><a class="btn btn-light border w-100 mt-2" href="${pageContext.request.contextPath}/discount-codes">Hủy sửa</a></c:if>
                        </form>
                    </div></div>
                </div>
                <div class="col-xl-8">
                    <div class="card border-0 shadow-sm">
                        <div class="card-header bg-white border-0 p-3 text-end">
                            <a class="btn btn-sm ${includeInactive?'btn-primary':'btn-outline-secondary'}" href="?includeInactive=${!includeInactive}">Hiện mã đã khóa</a>
                        </div>
                        <div class="table-responsive"><table class="table table-hover align-middle mb-0">
                            <thead class="table-light"><tr><th class="ps-4">Mã</th><th>Chương trình</th><th>Giá trị</th><th>Điều kiện</th><th>Lượt dùng</th><th>Trạng thái</th><th class="text-end pe-4"></th></tr></thead>
                            <tbody>
                            <c:forEach items="${codes}" var="code">
                                <tr class="${code.active?'':'opacity-75'}">
                                    <td class="ps-4"><span class="badge bg-primary fs-6"><c:out value="${code.code}"/></span></td>
                                    <td><div class="fw-semibold"><c:out value="${code.name}"/></div><small class="text-muted">${code.startsAt} → ${code.endsAt}</small></td>
                                    <td class="fw-semibold"><c:choose><c:when test="${code.discountType=='PERCENT'}">${code.discountValue}%</c:when><c:otherwise><fmt:formatNumber value="${code.discountValue}" pattern="#,##0"/> đ</c:otherwise></c:choose><c:if test="${not empty code.maximumDiscount}"><small class="d-block text-muted">Tối đa <fmt:formatNumber value="${code.maximumDiscount}" pattern="#,##0"/> đ</small></c:if></td>
                                    <td>Đơn từ <fmt:formatNumber value="${code.minimumOrder}" pattern="#,##0"/> đ</td>
                                    <td>${code.usedCount}<c:if test="${not empty code.usageLimit}"> / ${code.usageLimit}</c:if></td>
                                    <td><span class="badge ${code.active?'bg-success':'bg-secondary'}">${code.active?'Hoạt động':'Đã khóa'}</span></td>
                                    <td class="text-end pe-4 text-nowrap">
                                        <a class="btn btn-sm btn-outline-primary" href="?editId=${code.id}"><i class="fa-solid fa-pen"></i></a>
                                        <form method="post" class="d-inline" action="${pageContext.request.contextPath}/discount-codes"><input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}"><input type="hidden" name="id" value="${code.id}"><input type="hidden" name="action" value="${code.active?'lock':'unlock'}"><button class="btn btn-sm ${code.active?'btn-outline-danger':'btn-outline-success'}"><i class="fa-solid ${code.active?'fa-lock':'fa-lock-open'}"></i></button></form>
                                    </td>
                                </tr>
                            </c:forEach>
                            <c:if test="${empty codes}"><tr><td colspan="7" class="text-center text-muted py-5">Chưa có mã giảm giá.</td></tr></c:if>
                            </tbody>
                        </table></div>
                    </div>
                </div>
            </div>
        </div>
    </main>
</div>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>fetch('${pageContext.request.contextPath}/common/sidebar').then(r=>r.text()).then(html=>{document.getElementById('sidebar-placeholder').outerHTML=html;document.querySelector('.sidebar-nav a[href$="/discount-codes"]')?.classList.add('active');});</script>
</body>
</html>
