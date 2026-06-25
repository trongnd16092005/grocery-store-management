<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>Quản trị hệ thống</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css" rel="stylesheet">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/style.css">
    <style>
        .metric{border:0;border-radius:14px;box-shadow:0 4px 18px rgba(15,23,42,.06)}
        .metric-icon{width:44px;height:44px;border-radius:12px;display:grid;place-items:center}
        .store-code{font-family:Consolas,monospace;letter-spacing:.04em}
        .store-row{transition:background-color .15s,opacity .15s}
        .store-row.is-hidden{display:none}
        .empty-filter{display:none}
        .empty-filter.show{display:table-row}
        .action-buttons{white-space:nowrap}
        .detail-label{font-size:.78rem;color:#64748b;text-transform:uppercase;font-weight:700;letter-spacing:.04em}
    </style>
</head>
<body>
<div class="container-fluid p-0 d-flex">
    <%@ include file="common/sidebar.jspf" %>
    <main class="main-content">
        <div class="topbar d-flex justify-content-between align-items-center">
            <div><h1 class="h4 m-0 fw-bold">🛡️ Quản Trị Hệ Thống</h1><small class="text-muted">Quản lý các cửa hàng sử dụng nền tảng</small></div>
            <div class="text-end"><strong><c:out value="${sessionScope.currentUser.fullName}"/></strong><div class="small text-primary">Super Admin</div></div>
        </div>
        <div class="content">
            <c:if test="${not empty flashSuccess}"><div class="alert alert-success"><c:out value="${flashSuccess}"/></div></c:if>
            <c:if test="${not empty flashError}"><div class="alert alert-danger"><c:out value="${flashError}"/></div></c:if>
            <c:set var="totalUsers" value="0"/>
            <c:forEach items="${stores}" var="storeCount">
                <c:set var="totalUsers" value="${totalUsers + storeCount.adminCount + storeCount.employeeCount}"/>
            </c:forEach>
            <div class="row g-3 mb-4">
                <div class="col-sm-6 col-xl-3"><div class="card metric"><div class="card-body d-flex align-items-center gap-3"><div class="metric-icon bg-primary-subtle text-primary"><i class="fa-solid fa-store"></i></div><div><small class="text-muted">Tổng cửa hàng</small><div class="fs-2 fw-bold">${totalStores}</div></div></div></div></div>
                <div class="col-sm-6 col-xl-3"><div class="card metric"><div class="card-body d-flex align-items-center gap-3"><div class="metric-icon bg-success-subtle text-success"><i class="fa-solid fa-circle-check"></i></div><div><small class="text-muted">Đang hoạt động</small><div class="fs-2 fw-bold text-success">${activeStores}</div></div></div></div></div>
                <div class="col-sm-6 col-xl-3"><div class="card metric"><div class="card-body d-flex align-items-center gap-3"><div class="metric-icon bg-danger-subtle text-danger"><i class="fa-solid fa-lock"></i></div><div><small class="text-muted">Đã khóa</small><div class="fs-2 fw-bold text-danger">${inactiveStores}</div></div></div></div></div>
                <div class="col-sm-6 col-xl-3"><div class="card metric"><div class="card-body d-flex align-items-center gap-3"><div class="metric-icon bg-info-subtle text-info"><i class="fa-solid fa-users"></i></div><div><small class="text-muted">Tài khoản hoạt động</small><div class="fs-2 fw-bold">${totalUsers}</div></div></div></div></div>
            </div>
            <div class="card border-0 shadow-sm">
                <div class="card-header bg-white py-3">
                    <div class="d-flex flex-column flex-lg-row justify-content-between align-items-lg-center gap-3">
                        <div>
                            <h2 class="h6 fw-bold mb-1">Danh sách cửa hàng</h2>
                            <small class="text-muted"><span id="visibleStoreCount">${totalStores}</span> cửa hàng đang hiển thị</small>
                        </div>
                        <div class="d-flex flex-column flex-sm-row gap-2">
                            <div class="input-group">
                                <span class="input-group-text bg-white"><i class="fa-solid fa-magnifying-glass"></i></span>
                                <input id="storeSearch" class="form-control" placeholder="Tìm mã, tên, SĐT, địa chỉ..." autocomplete="off">
                                <button id="clearSearch" class="btn btn-outline-secondary d-none" type="button" title="Xóa tìm kiếm"><i class="fa-solid fa-xmark"></i></button>
                            </div>
                            <select id="statusFilter" class="form-select" aria-label="Lọc trạng thái">
                                <option value="all">Tất cả trạng thái</option>
                                <option value="active">Đang hoạt động</option>
                                <option value="inactive">Đã khóa</option>
                            </select>
                            <button id="refreshStores" class="btn btn-outline-primary text-nowrap" type="button">
                                <i class="fa-solid fa-rotate me-1"></i>Làm mới
                            </button>
                        </div>
                    </div>
                </div>
                <div class="table-responsive">
                    <table class="table table-hover align-middle mb-0" id="storeTable">
                        <thead class="table-light"><tr><th class="ps-4">Mã</th><th>Cửa hàng</th><th>Liên hệ</th><th class="text-center">ADMIN</th><th class="text-center">Nhân viên</th><th>Ngày đăng ký</th><th>Trạng thái</th><th class="text-end pe-4"></th></tr></thead>
                        <tbody>
                        <c:forEach items="${stores}" var="store">
                            <tr class="store-row ${store.active?'':'opacity-75'}"
                                data-status="${store.active?'active':'inactive'}"
                                data-search="<c:out value='${store.code} ${store.name} ${store.phone} ${store.address}'/>"
                                data-id="${store.id}"
                                data-code="<c:out value='${store.code}'/>"
                                data-name="<c:out value='${store.name}'/>"
                                data-phone="<c:out value='${store.phone}'/>"
                                data-address="<c:out value='${store.address}'/>"
                                data-admins="${store.adminCount}"
                                data-employees="${store.employeeCount}"
                                data-created="${store.createdAt}"
                                data-updated="${store.updatedAt}">
                                <td class="ps-4"><span class="store-code badge bg-light text-dark border"><c:out value="${store.code}"/></span></td>
                                <td><div class="fw-semibold"><c:out value="${store.name}"/></div><small class="text-muted"><c:out value="${store.address}"/></small></td>
                                <td><c:out value="${store.phone}" default="—"/></td>
                                <td class="text-center"><span class="badge bg-primary rounded-pill">${store.adminCount}</span></td>
                                <td class="text-center"><span class="badge bg-info text-dark rounded-pill">${store.employeeCount}</span></td>
                                <td class="small text-muted">${store.createdAt.toLocalDateTime()}</td>
                                <td><span class="badge ${store.active?'bg-success':'bg-secondary'}">${store.active?'Hoạt động':'Đã khóa'}</span></td>
                                <td class="text-end pe-4 action-buttons">
                                    <button type="button" class="btn btn-sm btn-outline-secondary me-1 btn-store-detail" title="Xem chi tiết">
                                        <i class="fa-solid fa-eye"></i>
                                    </button>
                                    <button type="button" class="btn btn-sm btn-outline-primary me-1 btn-copy-code" title="Sao chép mã cửa hàng">
                                        <i class="fa-regular fa-copy"></i>
                                    </button>
                                    <form method="post" action="${pageContext.request.contextPath}/super-admin" class="d-inline"
                                          onsubmit="return confirm('${store.active?'Khóa cửa hàng này? Tất cả phiên đăng nhập sẽ bị ngắt.':'Mở khóa cửa hàng này?'}')">
                                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                                        <input type="hidden" name="id" value="${store.id}">
                                        <input type="hidden" name="action" value="${store.active?'lock':'unlock'}">
                                        <button class="btn btn-sm ${store.active?'btn-outline-danger':'btn-outline-success'}">
                                            <i class="fa-solid ${store.active?'fa-lock':'fa-lock-open'} me-1"></i>${store.active?'Khóa':'Mở khóa'}
                                        </button>
                                    </form>
                                </td>
                            </tr>
                        </c:forEach>
                        <tr class="empty-filter"><td colspan="8" class="text-center text-muted py-5"><i class="fa-solid fa-magnifying-glass mb-2 fs-4 d-block"></i>Không tìm thấy cửa hàng phù hợp.</td></tr>
                        <c:if test="${empty stores}"><tr><td colspan="8" class="text-center text-muted py-5">Chưa có cửa hàng đăng ký.</td></tr></c:if>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </main>
</div>

<div class="modal fade" id="storeDetailModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow">
            <div class="modal-header">
                <div>
                    <h5 class="modal-title fw-bold" id="detailName">Chi tiết cửa hàng</h5>
                    <span class="badge bg-light text-dark border store-code" id="detailCode"></span>
                </div>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <div class="row g-3">
                    <div class="col-12"><div class="detail-label">Trạng thái</div><div id="detailStatus"></div></div>
                    <div class="col-sm-6"><div class="detail-label">Số điện thoại</div><div id="detailPhone">—</div></div>
                    <div class="col-sm-6"><div class="detail-label">Tài khoản hoạt động</div><div><strong id="detailUsers">0</strong> tài khoản</div></div>
                    <div class="col-12"><div class="detail-label">Địa chỉ</div><div id="detailAddress">—</div></div>
                    <div class="col-sm-6"><div class="detail-label">Quản trị viên</div><div id="detailAdmins">0</div></div>
                    <div class="col-sm-6"><div class="detail-label">Thu ngân</div><div id="detailEmployees">0</div></div>
                    <div class="col-sm-6"><div class="detail-label">Ngày đăng ký</div><div id="detailCreated">—</div></div>
                    <div class="col-sm-6"><div class="detail-label">Cập nhật gần nhất</div><div id="detailUpdated">—</div></div>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-outline-primary" id="copyDetailCode"><i class="fa-regular fa-copy me-1"></i>Sao chép mã</button>
                <button type="button" class="btn btn-light" data-bs-dismiss="modal">Đóng</button>
            </div>
        </div>
    </div>
</div>

<div class="toast-container position-fixed bottom-0 end-0 p-3">
    <div id="copyToast" class="toast" role="status">
        <div class="toast-body"><i class="fa-solid fa-circle-check text-success me-2"></i>Đã sao chép mã cửa hàng.</div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script>


const rows=[...document.querySelectorAll('.store-row')];
const search=document.getElementById('storeSearch');
const statusFilter=document.getElementById('statusFilter');
const clearSearch=document.getElementById('clearSearch');
const emptyFilter=document.querySelector('.empty-filter');
const visibleCount=document.getElementById('visibleStoreCount');
const detailModal=new bootstrap.Modal(document.getElementById('storeDetailModal'));
const copyToast=new bootstrap.Toast(document.getElementById('copyToast'),{delay:1800});
let selectedCode='';

function normalize(value){
    return (value||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();
}

function applyFilters(){
    const keyword=normalize(search.value.trim());
    const status=statusFilter.value;
    let count=0;
    rows.forEach(row=>{
        const matchesText=!keyword||normalize(row.dataset.search).includes(keyword);
        const matchesStatus=status==='all'||row.dataset.status===status;
        const visible=matchesText&&matchesStatus;
        row.classList.toggle('is-hidden',!visible);
        if(visible) count++;
    });
    visibleCount.textContent=count;
    emptyFilter?.classList.toggle('show',rows.length>0&&count===0);
    clearSearch.classList.toggle('d-none',!search.value);
}

function formatDate(value){
    if(!value) return '—';
    const date=new Date(value);
    return Number.isNaN(date.getTime())?'—':new Intl.DateTimeFormat('vi-VN',{
        dateStyle:'short',timeStyle:'short'
    }).format(date);
}

async function copyCode(code){
    if(!code) return;
    try{
        await navigator.clipboard.writeText(code);
    }catch(error){
        const input=document.createElement('textarea');
        input.value=code;
        document.body.appendChild(input);
        input.select();
        document.execCommand('copy');
        input.remove();
    }
    copyToast.show();
}

search.addEventListener('input',applyFilters);
statusFilter.addEventListener('change',applyFilters);
clearSearch.addEventListener('click',()=>{search.value='';search.focus();applyFilters();});
document.getElementById('refreshStores').addEventListener('click',()=>window.location.reload());
document.getElementById('copyDetailCode').addEventListener('click',()=>copyCode(selectedCode));

document.getElementById('storeTable').addEventListener('click',event=>{
    const row=event.target.closest('.store-row');
    if(!row) return;
    if(event.target.closest('.btn-copy-code')){
        copyCode(row.dataset.code);
        return;
    }
    if(!event.target.closest('.btn-store-detail')) return;
    selectedCode=row.dataset.code;
    document.getElementById('detailName').textContent=row.dataset.name||'Cửa hàng';
    document.getElementById('detailCode').textContent=row.dataset.code||'—';
    document.getElementById('detailPhone').textContent=row.dataset.phone||'—';
    document.getElementById('detailAddress').textContent=row.dataset.address||'—';
    document.getElementById('detailAdmins').textContent=row.dataset.admins||'0';
    document.getElementById('detailEmployees').textContent=row.dataset.employees||'0';
    document.getElementById('detailUsers').textContent=
        Number(row.dataset.admins||0)+Number(row.dataset.employees||0);
    document.getElementById('detailCreated').textContent=formatDate(row.dataset.created);
    document.getElementById('detailUpdated').textContent=formatDate(row.dataset.updated);
    document.getElementById('detailStatus').innerHTML=row.dataset.status==='active'
        ?'<span class="badge bg-success">Đang hoạt động</span>'
        :'<span class="badge bg-secondary">Đã khóa</span>';
    detailModal.show();
});
</script>
</body>
</html>