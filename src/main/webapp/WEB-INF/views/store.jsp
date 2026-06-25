<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Thông tin cửa hàng</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css?v=ui2">
</head>
<body>
<div class="container-fluid p-0 d-flex">
    <%@ include file="common/sidebar.jspf" %>
    <main class="main-content">
        <div class="topbar">
            <h1 class="h4 m-0 fw-bold"><i class="fa-solid fa-store me-2 text-primary"></i>Thông tin cửa hàng</h1>
        </div>
        <div class="content">
            <c:if test="${not empty flashSuccess}">
                <div class="alert alert-success"><c:out value="${flashSuccess}"/></div>
            </c:if>
            <c:if test="${not empty flashError}">
                <div class="alert alert-danger"><c:out value="${flashError}"/></div>
            </c:if>

            <div class="row justify-content-center">
                <div class="col-xl-10">
                    <div class="card border-0 shadow-sm">
                        <div class="card-body p-4 p-lg-5">
                            <div class="d-flex align-items-center gap-3 mb-4">
                                <div class="rounded-3 bg-primary-subtle text-primary p-3 fs-3">
                                    <i class="fa-solid fa-shop"></i>
                                </div>
                                <div>
                                    <h2 class="h5 fw-bold mb-1"><c:out value="${store.name}"/></h2>
                                    <div class="text-muted">Mã cửa hàng: <strong><c:out value="${store.code}"/></strong></div>
                                </div>
                            </div>

                            <ul class="nav nav-pills mb-4" role="tablist">
                                <li class="nav-item" role="presentation">
                                    <button class="nav-link active" data-bs-toggle="pill" data-bs-target="#storeInfoTab"
                                            type="button" role="tab">
                                        <i class="fa-solid fa-circle-info me-1"></i>Thông tin
                                    </button>
                                </li>
                                <li class="nav-item" role="presentation">
                                    <button class="nav-link" data-bs-toggle="pill" data-bs-target="#storeQrTab"
                                            type="button" role="tab">
                                        <i class="fa-solid fa-qrcode me-1"></i>Setup QR
                                        <span class="badge ms-1 ${store.payOsConfigured ? 'text-bg-success' : 'text-bg-secondary'}">
                                            ${store.payOsConfigured ? 'Đang bật' : 'Chưa bật'}
                                        </span>
                                    </button>
                                </li>
                            </ul>

                            <div class="tab-content">
                                <div class="tab-pane fade show active" id="storeInfoTab" role="tabpanel">
                                    <form method="post" action="${pageContext.request.contextPath}/store">
                                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="action" value="profile">
                                        <div class="mb-3">
                                            <label class="form-label fw-semibold">Mã cửa hàng</label>
                                            <input class="form-control bg-light" value="<c:out value='${store.code}'/>" disabled>
                                            <div class="form-text">Mã dùng khi nhân viên đăng nhập và không thể thay đổi.</div>
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label fw-semibold">Tên cửa hàng <span class="text-danger">*</span></label>
                                            <input class="form-control" name="name" maxlength="150" required
                                                   value="<c:out value='${store.name}'/>">
                                        </div>
                                        <div class="mb-3">
                                            <label class="form-label fw-semibold">Số điện thoại</label>
                                            <input class="form-control" name="phone" inputmode="numeric" maxlength="20"
                                                   value="<c:out value='${store.phone}'/>" placeholder="Ví dụ: 0901234567">
                                        </div>
                                        <div class="mb-4">
                                            <label class="form-label fw-semibold">Địa chỉ</label>
                                            <textarea class="form-control" name="address" rows="4" maxlength="500"
                                                      placeholder="Địa chỉ cửa hàng"><c:out value="${store.address}"/></textarea>
                                        </div>
                                        <button class="btn btn-primary">
                                            <i class="fa-solid fa-floppy-disk me-2"></i>Lưu thông tin
                                        </button>
                                    </form>
                                </div>

                                <div class="tab-pane fade" id="storeQrTab" role="tabpanel">
                                    <div class="alert alert-info d-flex gap-3">
                                        <i class="fa-solid fa-shield-halved fs-4 mt-1"></i>
                                        <div>
                                            <div class="fw-semibold">Cấu hình này dùng riêng cho cửa hàng <c:out value="${store.code}"/>.</div>
                                            <div class="small">Sau khi bật, POS sẽ tạo QR bằng bộ key này. Nếu để trống ô key khi lưu, hệ thống giữ key cũ.</div>
                                        </div>
                                    </div>

                                    <form method="post" action="${pageContext.request.contextPath}/store" autocomplete="off">
                                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="action" value="payos">

                                        <div class="form-check form-switch mb-4">
                                            <input class="form-check-input" type="checkbox" role="switch"
                                                   id="payosEnabled" name="payosEnabled"
                                                   ${store.payOsEnabled ? 'checked' : ''}>
                                            <label class="form-check-label fw-semibold" for="payosEnabled">
                                                Bật thanh toán QR payOS cho cửa hàng này
                                            </label>
                                        </div>

                                        <div class="row g-3">
                                            <div class="col-md-6">
                                                <label class="form-label fw-semibold">Client ID</label>
                                                <input class="form-control font-monospace" name="payosClientId"
                                                       maxlength="200"
                                                       placeholder="${empty store.payOsClientIdMask ? 'Nhập Client ID' : store.payOsClientIdMask}">
                                            </div>
                                            <div class="col-md-6">
                                                <label class="form-label fw-semibold">API Key</label>
                                                <input class="form-control font-monospace" name="payosApiKey"
                                                       maxlength="500" type="password"
                                                       placeholder="${empty store.payOsApiKeyMask ? 'Nhập API Key' : store.payOsApiKeyMask}">
                                            </div>
                                            <div class="col-12">
                                                <label class="form-label fw-semibold">Checksum Key</label>
                                                <input class="form-control font-monospace" name="payosChecksumKey"
                                                       maxlength="500" type="password"
                                                       placeholder="${empty store.payOsChecksumKeyMask ? 'Nhập Checksum Key' : store.payOsChecksumKeyMask}">
                                            </div>
                                        </div>

                                        <div class="d-flex flex-wrap align-items-center gap-2 mt-4">
                                            <button class="btn btn-primary">
                                                <i class="fa-solid fa-floppy-disk me-2"></i>Lưu cấu hình QR
                                            </button>
                                            <span class="small text-muted">
                                                Trạng thái:
                                                <strong class="${store.payOsConfigured ? 'text-success' : 'text-secondary'}">
                                                    ${store.payOsConfigured ? 'Sẵn sàng tạo QR' : 'Chưa sẵn sàng'}
                                                </strong>
                                            </span>
                                        </div>
                                    </form>
                                </div>
                            </div>
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
