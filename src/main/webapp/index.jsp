<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Quản Lý Cửa Hàng Bán Lẻ</title>
        <!-- Bootstrap 5 CSS -->
        <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
        <!-- FontAwesome -->
        <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css" rel="stylesheet">
        <!-- Đường dẫn gọi file CSS -->
        <link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/assets/css/style.css">
    </head>
    <body>

        <div class="container-fluid p-0 d-flex">
            <!-- THANH MENU BÊN TRÁI (SIDEBAR) -->
            <div id="sidebar-placeholder"></div>

            <!-- NỘI DUNG CHÍNH BÊN PHẢI (MAIN CONTENT) -->
            <div class="main-content">
                <div class="topbar">
                    <h1 class="h3 m-0">📊 TỔNG QUAN HỆ THỐNG (DASHBOARD)</h1>
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

        <!-- Bootstrap 5 JS Bundle -->
        <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
        <script>
            // Load sidebar and mark active item
            fetch('common/sidebar.html?v=3')
                .then(r => r.text())
                .then(html => {
                    document.getElementById('sidebar-placeholder').outerHTML = html;
                    // Mark active
                    const link = document.querySelector('.sidebar-nav a[href="index.jsp"]');
                    if (link) link.classList.add('active');
                });
        </script>
    </body>
</html>
