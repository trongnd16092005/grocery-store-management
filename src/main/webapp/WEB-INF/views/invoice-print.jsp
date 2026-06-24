<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>In hóa đơn ${invoice.code}</title>
    <style>
        *{box-sizing:border-box}
        body{margin:0;background:#eef2f7;color:#111;font-family:Arial,sans-serif;font-size:12px}
        .toolbar{position:sticky;top:0;padding:12px;text-align:center;background:#fff;border-bottom:1px solid #ddd}
        .toolbar button,.toolbar a{display:inline-block;border:0;border-radius:6px;padding:9px 15px;margin:0 3px;text-decoration:none;cursor:pointer}
        .print-btn{background:#2563eb;color:#fff}.close-btn{background:#e5e7eb;color:#111}
        .receipt{width:80mm;min-height:100mm;margin:18px auto;background:#fff;padding:6mm 5mm;box-shadow:0 5px 25px rgba(0,0,0,.12)}
        .center{text-align:center}.right{text-align:right}.bold{font-weight:700}
        h1{font-size:17px;margin:0 0 5px}h2{font-size:14px;margin:12px 0 5px}
        .muted{color:#555}.line{border-top:1px dashed #111;margin:8px 0}
        table{width:100%;border-collapse:collapse}th,td{padding:4px 1px;vertical-align:top}th{border-bottom:1px solid #111}
        .product{max-width:38mm}.summary td{padding:2px 1px}.total{font-size:15px}
        .footer{margin-top:14px}
        @page{size:80mm auto;margin:0}
        @media print{
            body{background:#fff}.toolbar{display:none}.receipt{width:80mm;margin:0;box-shadow:none;padding:5mm}
        }
    </style>
</head>
<body>
<div class="toolbar">
    <button class="print-btn" onclick="window.print()">🖨 In hóa đơn</button>
    <a class="close-btn" href="${pageContext.request.contextPath}/invoices?id=${invoice.id}">Quay lại</a>
</div>
<article class="receipt">
    <header class="center">
        <h1><c:out value="${store.name}"/></h1>
        <c:if test="${not empty store.address}"><div><c:out value="${store.address}"/></div></c:if>
        <c:if test="${not empty store.phone}"><div>Điện thoại: <c:out value="${store.phone}"/></div></c:if>
        <div class="line"></div>
        <h2>HÓA ĐƠN BÁN HÀNG</h2>
        <div class="bold"><c:out value="${invoice.code}"/></div>
    </header>
    <section>
        <div>Thời gian: ${invoice.createdAt.toLocalDateTime()}</div>
        <div>Thu ngân: <c:out value="${invoice.cashierName}" default="—"/></div>
        <div>Khách hàng: <c:out value="${invoice.customerName}" default="Khách lẻ"/></div>
        <div>Thanh toán: ${invoice.paymentMethod=='CASH'?'Tiền mặt':'QR'}</div>
    </section>
    <div class="line"></div>
    <table>
        <thead><tr><th class="product">Sản phẩm</th><th class="center">SL</th><th class="right">Đơn giá</th><th class="right">Tiền</th></tr></thead>
        <tbody>
        <c:forEach items="${invoice.details}" var="detail">
            <tr>
                <td class="product"><c:out value="${detail.productName}"/><div class="muted">${detail.productCode}</div></td>
                <td class="center">${detail.quantity}</td>
                <td class="right"><fmt:formatNumber value="${detail.unitPrice}" pattern="#,##0"/></td>
                <td class="right"><fmt:formatNumber value="${detail.lineTotal}" pattern="#,##0"/></td>
            </tr>
        </c:forEach>
        </tbody>
    </table>
    <div class="line"></div>
    <table class="summary">
        <tr><td>Tạm tính</td><td class="right"><fmt:formatNumber value="${invoice.subtotal}" pattern="#,##0"/> đ</td></tr>
        <c:if test="${invoice.discountAmount > 0}">
            <tr><td>Giảm giá <c:if test="${not empty invoice.discountCode}">(${invoice.discountCode})</c:if></td><td class="right">- <fmt:formatNumber value="${invoice.discountAmount}" pattern="#,##0"/> đ</td></tr>
        </c:if>
        <tr class="bold total"><td>TỔNG CỘNG</td><td class="right"><fmt:formatNumber value="${invoice.totalAmount}" pattern="#,##0"/> đ</td></tr>
        <c:if test="${invoice.paymentMethod=='CASH'}">
            <tr><td>Tiền khách đưa</td><td class="right"><fmt:formatNumber value="${invoice.cashReceived}" pattern="#,##0"/> đ</td></tr>
            <tr><td>Tiền thối</td><td class="right"><fmt:formatNumber value="${invoice.changeAmount}" pattern="#,##0"/> đ</td></tr>
        </c:if>
    </table>
    <footer class="center footer">
        <div class="line"></div>
        <div class="bold">Cảm ơn quý khách!</div>
        <div class="muted">Hẹn gặp lại</div>
    </footer>
</article>
</body>
</html>
