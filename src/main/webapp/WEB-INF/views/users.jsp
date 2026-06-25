<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width,initial-scale=1">
  <title>Tài khoản</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
  <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css" rel="stylesheet">
  <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
</head>
<body>
<div class="container-fluid p-0 d-flex">
  <%@ include file="common/sidebar.jspf" %>
  <main class="main-content">
    <div class="topbar d-flex justify-content-between align-items-center">
      <h1 class="h4 m-0 fw-bold">${isAdmin ? '🔐 Tài khoản & phân quyền' : '🔑 Bảo mật tài khoản'}</h1>
      <div class="d-flex gap-2">
        <%-- Nút đổi mật khẩu — hiển thị với mọi user --%>
        <button class="btn btn-outline-secondary" data-bs-toggle="modal" data-bs-target="#changePwModal">
          <i class="fa fa-key me-1"></i>Đổi mật khẩu
        </button>
        <%-- Nút tạo tài khoản — chỉ ADMIN --%>
        <c:if test="${isAdmin}">
          <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#createModal">
            <i class="fa fa-user-plus me-1"></i>Thêm tài khoản
          </button>
        </c:if>
      </div>
    </div>

    <div class="content">
      <c:if test="${mustChangePassword}">
        <div class="alert alert-warning">
          Đây là lần đăng nhập đầu tiên. Bạn phải đổi mật khẩu mặc định trước khi tiếp tục.
        </div>
      </c:if>
      <c:if test="${not empty flashSuccess}">
        <div class="alert alert-success alert-dismissible fade show" role="alert">
          ${flashSuccess}<button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
      </c:if>
      <c:if test="${not empty flashError}">
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
          <c:out value="${flashError}"/><button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
      </c:if>

      <c:if test="${isAdmin}">
      <div class="card border-0 shadow-sm">
        <div class="table-responsive">
          <table class="table table-hover align-middle mb-0">
            <thead class="table-light">
              <tr>
                <th class="ps-4">Họ tên</th>
                <th>Tên đăng nhập</th>
                <th>Vai trò</th>
                <th>Trạng thái</th>
                <th>Ngày tạo</th>
                <c:if test="${isAdmin}"><th class="text-end pe-4">Thao tác</th></c:if>
              </tr>
            </thead>
            <tbody>
              <c:forEach items="${users}" var="u">
                <tr>
                  <td class="ps-4 fw-semibold"><c:out value="${u.fullName}"/></td>
                  <td>${u.username}</td>
                  <td>
                    <span class="badge ${u.role=='SUPER_ADMIN'?'bg-dark':u.role=='ADMIN'?'bg-primary':'bg-info text-dark'}">
                      ${u.role=='SUPER_ADMIN'?'Super Admin':u.role=='ADMIN'?'Quản trị viên':'Thu ngân'}
                    </span>
                  </td>
                  <td>
                    <span class="badge ${u.active?'bg-success-subtle text-success':'bg-secondary'}">
                      ${u.active?'Hoạt động':'Đã khóa'}
                    </span>
                  </td>
                  <td>${u.createdAt.toLocalDate()}</td>
                  <c:if test="${isAdmin}">
                    <td class="text-end pe-4">
                      <%-- Nút sửa --%>
                      <button class="btn btn-sm btn-outline-primary me-1"
                              onclick="openEditModal(${u.id},'<c:out value="${u.fullName}" escapeXml="true"/>','${u.role}')">
                        <i class="fa fa-pencil"></i>
                      </button>
                      <%-- Nút đặt lại mật khẩu --%>
                      <button class="btn btn-sm btn-outline-warning me-1"
                              onclick="openResetPwModal(${u.id},'<c:out value="${u.username}" escapeXml="true"/>')">
                        <i class="fa fa-key"></i>
                      </button>
                      <%-- Nút khóa / mở khóa (ẩn với chính mình) --%>
                      <c:if test="${u.id != currentUserId}">
                        <c:choose>
                          <c:when test="${u.active}">
                            <form method="post" action="${pageContext.request.contextPath}/users"
                                  class="d-inline"
                                  onsubmit="return confirm('Khóa tài khoản ${u.username}?')">
                              <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                              <input type="hidden" name="action" value="lock">
                              <input type="hidden" name="id" value="${u.id}">
                              <button class="btn btn-sm btn-outline-danger">
                                <i class="fa fa-lock"></i>
                              </button>
                            </form>
                          </c:when>
                          <c:otherwise>
                            <form method="post" action="${pageContext.request.contextPath}/users"
                                  class="d-inline">
                              <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                              <input type="hidden" name="action" value="unlock">
                              <input type="hidden" name="id" value="${u.id}">
                              <button class="btn btn-sm btn-outline-success">
                                <i class="fa fa-lock-open"></i>
                              </button>
                            </form>
                          </c:otherwise>
                        </c:choose>
                      </c:if>
                    </td>
                  </c:if>
                </tr>
              </c:forEach>
            </tbody>
          </table>
        </div>
      </div>
      </c:if>
    </div>
  </main>
</div>

<%-- ===== Modal: Tạo tài khoản ===== --%>
<div class="modal fade" id="createModal" tabindex="-1">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content">
      <form method="post" action="${pageContext.request.contextPath}/users">
        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
        <input type="hidden" name="action" value="">
        <div class="modal-header">
          <h5 class="modal-title">Thêm tài khoản</h5>
          <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
          <div class="mb-3">
            <label class="form-label">Họ tên</label>
            <input class="form-control" name="fullName" required minlength="2">
          </div>
          <div class="mb-3">
            <label class="form-label">Tên đăng nhập</label>
            <input class="form-control" name="username" required minlength="3" pattern="[A-Za-z0-9._-]{3,50}">
          </div>
          <div class="mb-3">
            <label class="form-label">Mật khẩu</label>
            <input type="password" class="form-control" name="password" required minlength="8">
          </div>
          <div>
            <label class="form-label">Vai trò</label>
            <select class="form-select" name="role">
              <option value="CASHIER">Thu ngân</option>
              <option value="ADMIN">Quản trị viên</option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-light" data-bs-dismiss="modal">Hủy</button>
          <button class="btn btn-primary">Tạo tài khoản</button>
        </div>
      </form>
    </div>
  </div>
</div>

<%-- ===== Modal: Sửa tài khoản ===== --%>
<div class="modal fade" id="editModal" tabindex="-1">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content">
      <form method="post" action="${pageContext.request.contextPath}/users">
        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
        <input type="hidden" name="action" value="edit">
        <input type="hidden" name="id" id="editUserId">
        <div class="modal-header">
          <h5 class="modal-title">Chỉnh sửa tài khoản</h5>
          <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
          <div class="mb-3">
            <label class="form-label">Họ tên</label>
            <input class="form-control" name="fullName" id="editFullName" required minlength="2">
          </div>
          <div>
            <label class="form-label">Vai trò</label>
            <select class="form-select" name="role" id="editRole">
              <option value="CASHIER">Thu ngân</option>
              <option value="ADMIN">Quản trị viên</option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-light" data-bs-dismiss="modal">Hủy</button>
          <button class="btn btn-primary">Lưu thay đổi</button>
        </div>
      </form>
    </div>
  </div>
</div>

<%-- ===== Modal: Đổi mật khẩu (chính mình) ===== --%>
<div class="modal fade" id="changePwModal" tabindex="-1">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content">
      <form method="post" action="${pageContext.request.contextPath}/users" id="changePwForm">
        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
        <input type="hidden" name="action" value="change-password">
        <div class="modal-header">
          <h5 class="modal-title">Đổi mật khẩu</h5>
          <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
          <div class="mb-3">
            <label class="form-label">Mật khẩu hiện tại</label>
            <input type="password" class="form-control" name="currentPassword" required>
          </div>
          <div class="mb-3">
            <label class="form-label">Mật khẩu mới</label>
            <input type="password" class="form-control" name="newPassword" id="cpNewPw" required minlength="8">
          </div>
          <div>
            <label class="form-label">Xác nhận mật khẩu mới</label>
            <input type="password" class="form-control" name="confirmPassword" id="cpConfirm" required minlength="8">
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-light" data-bs-dismiss="modal">Hủy</button>
          <button class="btn btn-primary">Đổi mật khẩu</button>
        </div>
      </form>
    </div>
  </div>
</div>

<%-- ===== Modal: Đặt lại mật khẩu (admin reset) ===== --%>
<div class="modal fade" id="resetPwModal" tabindex="-1">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content">
      <form method="post" action="${pageContext.request.contextPath}/users" id="resetPwForm">
        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
        <input type="hidden" name="action" value="reset-password">
        <input type="hidden" name="id" id="resetPwUserId">
        <div class="modal-header">
          <h5 class="modal-title">Đặt lại mật khẩu — <span id="resetPwUsername" class="text-muted"></span></h5>
          <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
          <div class="mb-3">
            <label class="form-label">Mật khẩu mới</label>
            <input type="password" class="form-control" name="newPassword" id="rpNewPw" required minlength="8">
          </div>
          <div>
            <label class="form-label">Xác nhận mật khẩu mới</label>
            <input type="password" class="form-control" name="confirmPassword" id="rpConfirm" required minlength="8">
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-light" data-bs-dismiss="modal">Hủy</button>
          <button class="btn btn-warning">Đặt lại</button>
        </div>
      </form>
    </div>
  </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>
  // Sidebar
  

  // Mở modal sửa tài khoản
  function openEditModal(id, fullName, role) {
    document.getElementById('editUserId').value = id;
    document.getElementById('editFullName').value = fullName;
    document.getElementById('editRole').value = role;
    new bootstrap.Modal(document.getElementById('editModal')).show();
  }

  // Mở modal đặt lại mật khẩu (admin)
  function openResetPwModal(id, username) {
    document.getElementById('resetPwUserId').value = id;
    document.getElementById('resetPwUsername').textContent = username;
    document.getElementById('rpNewPw').value = '';
    document.getElementById('rpConfirm').value = '';
    new bootstrap.Modal(document.getElementById('resetPwModal')).show();
  }

  // Xác nhận mật khẩu mới khớp — form đổi mật khẩu bản thân
  document.getElementById('changePwForm').addEventListener('submit', function(e) {
    const pw = document.getElementById('cpNewPw').value;
    const cf = document.getElementById('cpConfirm').value;
    if (pw !== cf) { e.preventDefault(); alert('Mật khẩu xác nhận không khớp.'); }
  });

  // Xác nhận mật khẩu mới khớp — form reset mật khẩu
  document.getElementById('resetPwForm').addEventListener('submit', function(e) {
    const pw = document.getElementById('rpNewPw').value;
    const cf = document.getElementById('rpConfirm').value;
    if (pw !== cf) { e.preventDefault(); alert('Mật khẩu xác nhận không khớp.'); }
  });
</script>
</body>
</html>