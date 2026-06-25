<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!doctype html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Tổng quan cửa hàng</title>
    <link
            href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
            rel="stylesheet">
    <link
            href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css"
            rel="stylesheet">
    <link
            href="${pageContext.request.contextPath}/assets/css/style.css?v=ui2"
            rel="stylesheet">
    <link
            href="${pageContext.request.contextPath}/assets/css/dashboard.css?v=ui2"
            rel="stylesheet">
</head>
<body>
<div class="container-fluid p-0 d-flex">
    <%@ include file="common/sidebar.jspf" %>

    <main class="main-content">
        <div class="topbar d-flex justify-content-between align-items-center">
            <div>
                <h1 class="h4 m-0 fw-bold">Tổng quan cửa hàng</h1>
                <small class="text-muted">Doanh thu và hoạt động bán hàng gần đây</small>
            </div>
            <div class="text-end">
                <div class="fw-semibold">
                    <c:out value="${sessionScope.currentUser.fullName}"/>
                </div>
                <small class="text-muted">
                    ${sessionScope.currentUser.role == 'ADMIN'
                            ? 'Quản trị viên' : 'Thu ngân'}
                </small>
            </div>
        </div>

        <div class="content">
            <section class="row g-3 mb-4" aria-label="Chỉ số tổng quan">
                <div class="col-sm-6 col-xl-3">
                    <div class="card metric">
                        <div class="card-body d-flex gap-3">
                            <div class="metric-icon bg-primary-subtle text-primary">
                                <i class="fa-solid fa-box"></i>
                            </div>
                            <div>
                                <small class="text-muted">Sản phẩm đang bán</small>
                                <div class="metric-value">${stats.productCount}</div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-sm-6 col-xl-3">
                    <div class="card metric">
                        <div class="card-body d-flex gap-3">
                            <div class="metric-icon bg-success-subtle text-success">
                                <i class="fa-solid fa-coins"></i>
                            </div>
                            <div>
                                <small class="text-muted">Doanh thu hôm nay</small>
                                <div class="metric-value">
                                    <fmt:formatNumber
                                            value="${stats.todayRevenue}"
                                            pattern="#,##0"/> đ
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-sm-6 col-xl-3">
                    <div class="card metric">
                        <div class="card-body d-flex gap-3">
                            <div class="metric-icon bg-info-subtle text-info">
                                <i class="fa-solid fa-receipt"></i>
                            </div>
                            <div>
                                <small class="text-muted">Hóa đơn hôm nay</small>
                                <div class="metric-value">${stats.todayInvoiceCount}</div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col-sm-6 col-xl-3">
                    <div class="card metric">
                        <div class="card-body d-flex gap-3">
                            <div class="metric-icon bg-warning-subtle text-warning">
                                <i class="fa-solid fa-triangle-exclamation"></i>
                            </div>
                            <div>
                                <small class="text-muted">Sắp/hết hàng</small>
                                <div class="metric-value">${stats.lowStockCount}</div>
                            </div>
                        </div>
                    </div>
                </div>
            </section>

            <section class="row g-4 mb-4" aria-label="Biểu đồ kinh doanh">
                <div class="col-xl-8">
                    <div class="card dashboard-card h-100">
                        <div class="card-header chart-header">
                            <div>
                                <h2>Doanh thu 7 ngày gần nhất</h2>
                                <p>Chỉ tính các hóa đơn đã thanh toán</p>
                            </div>
                            <div class="chart-total">
                                <span>Tổng 7 ngày</span>
                                <strong>
                                    <fmt:formatNumber
                                            value="${stats.sevenDayRevenue}"
                                            pattern="#,##0"/> đ
                                </strong>
                            </div>
                        </div>
                        <div class="card-body">
                            <div class="revenue-chart">
                                <c:forEach
                                        items="${stats.sevenDayRevenuePoints}"
                                        var="point">
                                    <fmt:formatNumber
                                            var="pointRevenue"
                                            value="${point.revenue}"
                                            pattern="#,##0"/>
                                    <div class="revenue-column">
                                        <div class="bar-track">
                                            <div
                                                    class="revenue-bar ${point.chartPercent == 0
                                                            ? 'is-empty' : ''}"
                                                    style="height:${point.chartPercent}%"
                                                    title="${point.label}: ${pointRevenue} đ">
                                                <span>${pointRevenue} đ</span>
                                            </div>
                                        </div>
                                        <div class="bar-label">${point.label}</div>
                                    </div>
                                </c:forEach>
                            </div>
                        </div>
                    </div>
                </div>

                <div class="col-xl-4">
                    <div class="card dashboard-card h-100">
                        <div class="card-header chart-header">
                            <div>
                                <h2>Phương thức thanh toán</h2>
                                <p>Trong 30 ngày gần nhất</p>
                            </div>
                        </div>
                        <div class="card-body payment-chart-body">
                            <div
                                    class="payment-donut
                                        ${(stats.cashPaymentCount + stats.qrPaymentCount) == 0
                                                ? 'is-empty' : ''}"
                                    style="background:conic-gradient(
                                            #2563eb 0 ${stats.cashPaymentPercent}%,
                                            #14b8a6 ${stats.cashPaymentPercent}% 100%)">
                                <div class="donut-center">
                                    <strong>
                                        ${stats.cashPaymentCount + stats.qrPaymentCount}
                                    </strong>
                                    <span>hóa đơn</span>
                                </div>
                            </div>

                            <div class="payment-legend">
                                <div class="legend-row">
                                    <span class="legend-label">
                                        <i class="legend-dot cash"></i>Tiền mặt
                                    </span>
                                    <span>
                                        <strong>${stats.cashPaymentCount}</strong>
                                        <small>${stats.cashPaymentPercent}%</small>
                                    </span>
                                </div>
                                <div class="legend-row">
                                    <span class="legend-label">
                                        <i class="legend-dot qr"></i>QR payOS
                                    </span>
                                    <span>
                                        <strong>${stats.qrPaymentCount}</strong>
                                        <small>${stats.qrPaymentPercent}%</small>
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </section>

            <section class="row g-4" aria-label="Hoạt động gần đây">
                <div class="col-xl-8">
                    <div class="card dashboard-card">
                        <div class="card-header table-card-header">
                            <h2>Hóa đơn gần đây</h2>
                            <a
                                    href="${pageContext.request.contextPath}/invoices"
                                    class="btn btn-sm btn-outline-primary">
                                Xem tất cả
                            </a>
                        </div>
                        <div class="table-responsive">
                            <table class="table table-hover align-middle mb-0">
                                <thead class="table-light">
                                <tr>
                                    <th class="ps-4">Mã</th>
                                    <th>Khách hàng</th>
                                    <th>Thanh toán</th>
                                    <th>Tổng tiền</th>
                                    <th>Trạng thái</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach items="${stats.recentInvoices}" var="invoice">
                                    <tr>
                                        <td class="ps-4">
                                            <a
                                                    href="${pageContext.request.contextPath}/invoices?id=${invoice.id}"
                                                    class="fw-semibold text-decoration-none">
                                                ${invoice.code}
                                            </a>
                                        </td>
                                        <td>
                                            <c:out
                                                    value="${invoice.customerName}"
                                                    default="Khách lẻ"/>
                                        </td>
                                        <td>${invoice.paymentMethod}</td>
                                        <td class="fw-semibold">
                                            <fmt:formatNumber
                                                    value="${invoice.totalAmount}"
                                                    pattern="#,##0"/> đ
                                        </td>
                                        <td>
                                            <span class="badge ${invoice.status == 'PAID'
                                                    ? 'bg-success-subtle text-success'
                                                    : invoice.status == 'PENDING'
                                                    ? 'bg-warning-subtle text-warning'
                                                    : 'bg-danger-subtle text-danger'}">
                                                ${invoice.status == 'PAID'
                                                        ? 'Đã thanh toán'
                                                        : invoice.status == 'PENDING'
                                                        ? 'Chờ QR' : 'Đã hủy'}
                                            </span>
                                        </td>
                                    </tr>
                                </c:forEach>
                                <c:if test="${empty stats.recentInvoices}">
                                    <tr>
                                        <td
                                                colspan="5"
                                                class="text-center text-muted py-5">
                                            Chưa có hóa đơn.
                                        </td>
                                    </tr>
                                </c:if>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>

                <div class="col-xl-4">
                    <div class="card dashboard-card">
                        <div class="card-body quick-actions">
                            <h2>Thao tác nhanh</h2>
                            <div class="d-grid gap-2">
                                <a
                                        class="btn btn-primary"
                                        href="${pageContext.request.contextPath}/sale">
                                    <i class="fa-solid fa-cash-register me-2"></i>
                                    Mở màn hình bán hàng
                                </a>
                                <a
                                        class="btn btn-outline-secondary"
                                        href="${pageContext.request.contextPath}/customers">
                                    <i class="fa-solid fa-users me-2"></i>
                                    Tra cứu khách hàng
                                </a>
                                <c:if test="${sessionScope.currentUser.role == 'ADMIN'}">
                                    <a
                                            class="btn btn-outline-warning"
                                            href="${pageContext.request.contextPath}/inventory">
                                        <i class="fa-solid fa-warehouse me-2"></i>
                                        Xử lý tồn kho (${stats.lowStockCount})
                                    </a>
                                </c:if>
                            </div>
                            <div class="customer-summary">
                                <span>Khách hàng đang hoạt động</span>
                                <strong>${stats.customerCount}</strong>
                            </div>
                        </div>
                    </div>

                    <div class="card dashboard-card mt-4">
                        <div class="card-header table-card-header">
                            <h2>Cảnh báo tồn kho</h2>
                            <a
                                    href="${pageContext.request.contextPath}/inventory"
                                    class="btn btn-sm btn-outline-warning">
                                Xử lý
                            </a>
                        </div>
                        <div class="card-body stock-alert-list">
                            <c:forEach
                                    items="${stats.lowStockProducts}"
                                    var="product">
                                <div class="stock-alert-item">
                                    <div class="stock-alert-icon ${product.outOfStock
                                            ? 'is-out' : 'is-low'}">
                                        <i class="fa-solid ${product.outOfStock
                                                ? 'fa-circle-exclamation'
                                                : 'fa-triangle-exclamation'}"></i>
                                    </div>
                                    <div class="stock-alert-info">
                                        <div class="fw-semibold">
                                            <c:out value="${product.name}"/>
                                        </div>
                                        <small>
                                            ${product.code}
                                            <c:if test="${not empty product.categoryName}">
                                                · <c:out value="${product.categoryName}"/>
                                            </c:if>
                                        </small>
                                    </div>
                                    <div class="stock-alert-count">
                                        <strong>${product.stockQuantity}</strong>
                                        <small>/ ${product.minimumStock}</small>
                                    </div>
                                </div>
                            </c:forEach>
                            <c:if test="${empty stats.lowStockProducts}">
                                <div class="stock-alert-empty">
                                    <i class="fa-solid fa-circle-check"></i>
                                    <span>Kho đang ổn, chưa có sản phẩm cần nhập.</span>
                                </div>
                            </c:if>
                        </div>
                    </div>
                </div>
            </section>
        </div>
    </main>
</div>

<script
        src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js">
</script>
</body>
</html>
