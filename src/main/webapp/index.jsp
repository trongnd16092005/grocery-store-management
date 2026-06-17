<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Quản Lý Cửa Hàng Bán Lẻ</title>
        <!-- Đường dẫn gọi file CSS -->
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/assets/css/style.css">
    </head>
    <body>

        <div class="container">
            <!-- THANH MENU BÊN TRÁI (SIDEBAR) -->
            <div class="sidebar">
                <div class="logo">Cửa Hàng ABC</div>
                <ul class="sidebar-nav">
                    <li><a href="index.jsp" class="active">📊 Dashboard</a></li>
                    <li><a href="login.jsp">🔑 Đăng xuất</a></li>
                    <li><a href="products.jsp">📦 Sản phẩm</a></li>
                    <li><a href="categories.jsp">📁 Danh mục</a></li>
                    <li><a href="sale.jsp">🛒 Bán hàng</a></li>
                    <li><a href="invoices.jsp">🧾 Hóa đơn</a></li>
                </ul>
            </div>

            <!-- NỘI DUNG CHÍNH BÊN PHẢI (MAIN CONTENT) -->
            <div class="main-content">
                <div class="topbar">
                    <h1>📊 TỔNG QUAN HỆ THỐNG (DASHBOARD)</h1>
                </div>

                <div class="content">
                    <!-- Thẻ thông số nhanh (Cards) -->
                    <div class="cards">
                        <div class="card">
                            <h3>Sản phẩm</h3>
                            <div class="card-value">150</div>
                        </div>
                        <div class="card">
                            <h3>Doanh thu</h3>
                            <div class="card-value">1,250,000 đ</div>
                        </div>
                        <div class="card">
                            <h3>Đơn hàng mới</h3>
                            <div class="card-value">12</div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

    </body>
</html>