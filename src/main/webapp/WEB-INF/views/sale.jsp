<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" isELIgnored="true" %>
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Bán Hàng (POS) - Cửa Hàng Bán Lẻ</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css" rel="stylesheet">
    <link rel="stylesheet" href="<%= request.getContextPath() %>/assets/css/style.css?v=ui2">
    <style>
        /* POS-specific styles */
        body { overflow: hidden; }

        .pos-wrapper {
            display: flex;
            flex: 1;
            min-height: 0;
            overflow: hidden;
        }

        /* Product Grid Panel */
        .product-panel {
            flex: 1;
            min-width: 0;
            min-height: 0;
            display: flex;
            flex-direction: column;
            overflow: hidden;
            border-right: 1px solid rgba(226, 232, 240, .82);
            background:
                radial-gradient(circle at top left, rgba(37, 99, 235, .08), transparent 38%),
                #f6f9fc;
        }

        .product-panel-filter {
            padding: 14px 18px;
            background: rgba(255, 255, 255, .88);
            border-bottom: 1px solid #e6edf5;
            backdrop-filter: blur(12px);
            flex-shrink: 0;
        }

        .product-grid {
            flex: 1;
            overflow-y: auto;
            padding: 18px;
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(155px, 1fr));
            grid-auto-rows: 132px;
            gap: 14px;
            align-content: start;
        }

        .product-card {
            background: rgba(255, 255, 255, .96);
            border: 1px solid #e1e9f2;
            border-radius: 16px;
            cursor: pointer;
            transition: transform 0.18s, box-shadow 0.18s, border-color 0.18s;
            overflow: hidden;
            display: flex;
            flex-direction: column;
            box-shadow: 0 8px 22px rgba(15, 23, 42, .04);
            min-height: 132px;
        }
        .product-card:hover {
            transform: translateY(-3px);
            box-shadow: 0 14px 30px rgba(37, 99, 235, .12);
            border-color: #9bbcff;
        }
        .product-card.out-of-stock {
            opacity: 0.55;
            cursor: not-allowed;
        }
        .product-card.out-of-stock:hover {
            transform: none;
            box-shadow: none;
            border-color: #e2e8f0;
        }
        .product-card .icon-area {
            height: 42px;
            min-height: 42px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 1.45rem;
            background: linear-gradient(135deg, #eff6ff, #f8fafc) !important;
            color: #2563eb !important;
        }
        .product-card .card-info {
            flex: 1;
            min-height: 0;
            padding: 9px 10px 10px;
            display: flex;
            flex-direction: column;
        }
        .product-card .card-info .cat-label {
            font-size: 10px;
            text-transform: uppercase;
            font-weight: 600;
            letter-spacing: 0.5px;
            color: #94a3b8;
        }
        .product-card .card-info .prod-name {
            font-size: 13px;
            font-weight: 700;
            color: #1e293b;
            line-height: 1.3;
            margin: 2px 0 6px;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }
        .product-card .card-info > .d-flex {
            margin-top: auto;
            gap: 8px;
        }
        .product-card .card-info .prod-price {
            font-size: 14px;
            font-weight: 700;
            color: #ef4444;
            white-space: nowrap;
        }
        .product-card .badge {
            flex: 0 0 auto;
            padding: .28rem .5rem;
            font-size: 10px !important;
            line-height: 1.1;
            white-space: nowrap;
        }
        .product-skeleton {
            min-height: 132px;
            padding: 12px;
            border: 1px solid #e6edf5;
            border-radius: 16px;
            background: rgba(255, 255, 255, .88);
            box-shadow: 0 8px 22px rgba(15, 23, 42, .04);
        }
        .skeleton-line,
        .skeleton-icon {
            position: relative;
            overflow: hidden;
            background: #e8eef6;
            border-radius: 999px;
        }
        .skeleton-icon {
            width: 34px;
            height: 34px;
            margin: 0 auto 14px;
            border-radius: 11px;
        }
        .skeleton-line {
            height: 10px;
            margin-bottom: 10px;
        }
        .skeleton-line.short { width: 46%; }
        .skeleton-line.medium { width: 74%; }
        .skeleton-line.full { width: 100%; }
        .skeleton-line.price {
            width: 52%;
            height: 13px;
            margin-top: 16px;
            margin-bottom: 0;
        }
        .skeleton-line::after,
        .skeleton-icon::after {
            content: "";
            position: absolute;
            inset: 0;
            transform: translateX(-100%);
            background: linear-gradient(90deg, transparent, rgba(255,255,255,.72), transparent);
            animation: skeletonShimmer 1.15s infinite;
        }
        @keyframes skeletonShimmer {
            100% { transform: translateX(100%); }
        }
        .product-load-error {
            grid-column: 1 / -1;
            align-self: start;
        }

        /* Cart Panel */
        .cart-panel {
            width: 376px;
            min-height: 0;
            flex-shrink: 0;
            display: flex;
            flex-direction: column;
            background: #ffffff;
            overflow-y: auto;
            overscroll-behavior: contain;
            scrollbar-gutter: stable;
            box-shadow: -12px 0 34px rgba(15, 23, 42, .06);
        }
        .cart-header {
            padding: 16px 20px;
            border-bottom: 1px solid #edf2f7;
            display: flex;
            justify-content: space-between;
            align-items: center;
            flex-shrink: 0;
        }
        .cart-items {
            flex: 1;
            min-height: 100px;
            overflow-y: auto;
            padding: 8px 0;
        }
        .cart-item {
            display: flex;
            align-items: center;
            padding: 12px 18px;
            gap: 10px;
            border-bottom: 1px solid #edf2f7;
        }
        .cart-item:last-child { border-bottom: none; }
        .cart-item .item-info { flex: 1; min-width: 0; }
        .cart-item .item-name {
            font-size: 13px; font-weight: 600;
            color: #1e293b; white-space: nowrap;
            overflow: hidden; text-overflow: ellipsis;
        }
        .cart-item .item-price { font-size: 12px; color: #ef4444; }
        .qty-ctrl {
            display: flex; align-items: center; gap: 6px;
        }
        .qty-ctrl button {
            width: 26px; height: 26px;
            border-radius: 50%; border: 1.5px solid #cbd5e1;
            background: #f8fafc; font-size: 14px; font-weight: 700;
            color: #475569; cursor: pointer; line-height: 1;
            display: flex; align-items: center; justify-content: center;
            transition: background 0.15s, border-color 0.15s;
        }
        .qty-ctrl button:hover { background: #e2e8f0; border-color: #94a3b8; }
        .qty-ctrl .qty-val {
            font-size: 14px; font-weight: 700;
            min-width: 22px; text-align: center;
        }
        .qty-ctrl .qty-input {
            width: 48px;
            height: 28px;
            border: 1px solid #cbd5e1;
            border-radius: 8px;
            text-align: center;
            font-weight: 700;
            padding: 2px 4px;
        }
        .item-subtotal {
            font-size: 13px; font-weight: 700;
            color: #1e293b; min-width: 80px; text-align: right;
        }
        .btn-remove-item {
            background: none; border: none; color: #94a3b8;
            cursor: pointer; font-size: 14px; padding: 2px 4px;
            transition: color 0.15s;
        }
        .btn-remove-item:hover { color: #ef4444; }

        .cart-empty {
            flex: 1; display: flex; flex-direction: column;
            align-items: center; justify-content: center;
            color: #cbd5e1; padding: 40px;
        }

        .cart-summary {
            flex-shrink: 0;
            border-top: 1px solid #e2e8f0;
            padding: 16px 20px 18px;
            background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
        }
        .cart-panel::-webkit-scrollbar,
        .cart-items::-webkit-scrollbar,
        .product-grid::-webkit-scrollbar { width: 7px; }
        .cart-panel::-webkit-scrollbar-thumb,
        .cart-items::-webkit-scrollbar-thumb,
        .product-grid::-webkit-scrollbar-thumb {
            background: #cbd5e1;
            border-radius: 99px;
        }
        .checkout-btn {
            font-size: 1.1rem; font-weight: 700;
            padding: 13px; border-radius: 13px;
            letter-spacing: 0.2px;
        }
        .payment-method {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 8px;
        }
        .payment-option {
            border: 1px solid #d8e2ee;
            background: #fff;
            color: #475569;
            border-radius: 12px;
            padding: 9px 8px;
            font-size: 13px;
            font-weight: 600;
        }
        .payment-option.active {
            border-color: #93b4ff;
            background: linear-gradient(180deg, #eff6ff, #ffffff);
            color: #2563eb;
        }
        .qr-placeholder {
            border: 1px dashed #93c5fd;
            background: #eff6ff;
            border-radius: 14px;
            padding: 12px;
            text-align: center;
        }
        #qrCanvas { width:210px;height:210px;max-width:100%;margin:0 auto;overflow:hidden }
        #qrCanvas canvas,#qrCanvas img,#qrCanvas svg { width:100%!important;height:100%!important;display:block }
        .qr-waiting { animation: qrPulse 1.5s ease-in-out infinite; }
        @keyframes qrPulse { 50% { opacity: .55; } }
    </style>
</head>
<body>

<div class="container-fluid p-0 d-flex" style="height:100vh; overflow:hidden;">
    <!-- SIDEBAR -->
    <%@ include file="common/sidebar.jspf" %>

    <!-- MAIN CONTENT -->
    <div class="main-content" style="height:100vh; min-height:0; overflow:hidden;">
        <div class="topbar d-flex justify-content-between align-items-center">
            <h1 class="h4 m-0 fw-bold"><i class="fa-solid fa-cash-register me-2 text-primary"></i>Bán hàng POS</h1>
            <button type="button" class="btn btn-primary" onclick="openQrPayment()">
                <i class="fa-solid fa-qrcode me-2"></i>Thanh toán QR
            </button>
        </div>

        <div class="pos-wrapper">
            <!-- ===== LEFT: Product Grid ===== -->
            <div class="product-panel">
                <!-- Filter Bar -->
                <div class="product-panel-filter">
                    <div class="row g-2">
                        <div class="col-7">
                            <div class="input-group input-group-sm">
                                <span class="input-group-text bg-white border-end-0 text-muted">
                                    <i class="fa-solid fa-magnifying-glass"></i>
                                </span>
                                <input type="text" id="searchInput" class="form-control border-start-0 ps-0"
                                    placeholder="Tìm sản phẩm..." onkeyup="filterProducts()">
                            </div>
                        </div>
                        <div class="col-5">
                            <select class="form-select form-select-sm" id="categoryFilter" onchange="filterProducts()">
                                <option value="">Tất cả danh mục</option>
                                <option>Nước ngọt</option>
                                <option>Bánh kẹo</option>
                                <option>Mì ăn liền</option>
                                <option>Gia vị</option>
                            </select>
                        </div>
                    </div>
                </div>

                <!-- Product Cards -->
                <div class="product-grid" id="productGrid">
                    <!-- Cards generated by JS from productData -->
                </div>
            </div>

            <!-- ===== RIGHT: Cart ===== -->
            <div class="cart-panel">
                <!-- Cart Header -->
                <div class="cart-header">
                    <div>
                        <h5 class="m-0 fw-bold"><i class="fa-solid fa-shopping-basket text-primary me-2"></i>Giỏ hàng</h5>
                        <small class="text-muted" id="cartCount">0 sản phẩm</small>
                    </div>
                    <button class="btn btn-sm btn-outline-danger" onclick="clearCart()">
                        <i class="fa-solid fa-trash-can me-1"></i>Xóa tất cả
                    </button>
                </div>

                <!-- Cart Empty State -->
                <div class="cart-empty" id="cartEmpty">
                    <i class="fa-solid fa-cart-shopping fa-4x mb-3"></i>
                    <p class="mb-0 fw-semibold">Giỏ hàng trống</p>
                    <p class="small mt-1">Nhấn vào sản phẩm để thêm vào giỏ</p>
                </div>

                <!-- Cart Items -->
                <div class="cart-items d-none" id="cartItems"></div>

                <!-- Cart Summary & Checkout -->
                <div class="cart-summary">
                    <div class="d-flex justify-content-between mb-2">
                        <span class="text-muted">Số mặt hàng:</span>
                        <span class="fw-semibold" id="summaryItemCount">0</span>
                    </div>
                    <div class="d-flex justify-content-between mb-2">
                        <span class="text-muted">Tạm tính:</span>
                        <span class="fw-semibold" id="subtotalAmount">0 đ</span>
                    </div>

                    <div class="mb-2">
                        <div class="d-flex justify-content-between align-items-center mb-1">
                            <span class="text-muted">Mã giảm giá:</span>
                            <span class="fw-semibold text-success" id="discountAmountDisplay">0 đ</span>
                        </div>
                        <div class="input-group input-group-sm">
                            <span class="input-group-text bg-white"><i class="fa-solid fa-ticket text-muted"></i></span>
                            <input type="text" id="discountCodeInput" class="form-control text-uppercase"
                                   placeholder="Ví dụ: SALE10" oninput="onDiscountCodeInput(this)">
                            <button type="button" class="btn btn-outline-primary" onclick="applyDiscountCode()">Áp dụng</button>
                        </div>
                        <div class="small mt-1 d-none" id="discountFeedback"></div>
                    </div>

                    <div class="d-flex justify-content-between mb-3">
                        <span class="fw-bold fs-5">Tổng tiền:</span>
                        <span class="fw-bold fs-5 text-danger" id="totalAmount">0 đ</span>
                    </div>

                    <div class="mb-3">
                        <label class="form-label small fw-semibold text-muted mb-1" for="customerCodeInput">
                            Mã khách hàng <span class="fw-normal">(không bắt buộc)</span>
                        </label>
                        <div class="input-group input-group-sm">
                            <span class="input-group-text bg-white"><i class="fa-solid fa-user-tag text-muted"></i></span>
                            <input type="text" id="customerCodeInput" class="form-control text-uppercase"
                                placeholder="Ví dụ: KH001" oninput="onCustomerCodeInput(this)">
                            <button class="btn btn-outline-primary" type="button" onclick="lookupCustomer()">Kiểm tra</button>
                        </div>
                        <div class="small mt-1 d-none" id="customerFeedback"></div>
                    </div>

                    <div class="mb-2 d-none" id="pointsPanel">
                        <div class="d-flex justify-content-between align-items-center mb-1">
                            <span class="text-muted">Dùng điểm:</span>
                            <span class="fw-semibold text-success" id="pointsDiscountDisplay">0 đ</span>
                        </div>
                        <div class="input-group input-group-sm">
                            <span class="input-group-text bg-white">
                                <i class="fa-solid fa-star text-warning"></i>
                            </span>
                            <input type="number" min="0" id="pointsInput" class="form-control"
                                   placeholder="Số điểm muốn dùng" oninput="onPointsInput(this)">
                            <button type="button" class="btn btn-outline-secondary" onclick="useMaxPoints()">
                                Dùng tối đa
                            </button>
                        </div>
                        <div class="small text-muted mt-1" id="pointsHint">
                            Chọn khách hàng để xem ưu đãi điểm.
                        </div>
                    </div>

                    <div class="mb-3">
                        <div class="small fw-semibold text-muted mb-2">Phương thức thanh toán</div>
                        <div class="payment-method">
                            <button type="button" class="payment-option active" id="cashMethodBtn" onclick="setPaymentMethod('cash')">
                                <i class="fa-solid fa-money-bill-wave me-1"></i>Tiền mặt
                            </button>
                            <button type="button" class="payment-option" id="qrMethodBtn" onclick="setPaymentMethod('qr')">
                                <i class="fa-solid fa-qrcode me-1"></i>Quét QR
                            </button>
                        </div>
                    </div>

                    <!-- Cash Input -->
                    <div id="cashPaymentPanel">
                    <div class="mb-2">
                        <label class="form-label small fw-semibold text-muted mb-1">
                            Tiền khách đưa (đ) <span class="text-danger">*</span>
                        </label>
                        <input type="text" id="cashInput" class="form-control text-end fw-bold fs-5"
                            placeholder="0" oninput="onCashInput(this)">
                        <div class="invalid-feedback" id="cashError">
                            Số tiền chưa đủ để thanh toán.
                        </div>
                    </div>

                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <span class="text-muted small fw-semibold">Tiền thối lại:</span>
                        <span class="fw-bold text-success fs-5" id="changeAmount">0 đ</span>
                    </div>
                    </div>

                    <div class="qr-placeholder mb-3 d-none" id="qrPaymentPanel">
                        <div id="qrReadyState">
                            <i class="fa-solid fa-qrcode text-primary fs-1 mb-2"></i>
                            <div class="fw-bold">Thanh toán QR tự động</div>
                            <div class="small text-muted">Chế độ kiểm thử: QR payOS sẽ tạo với số tiền 5.000đ.</div>
                        </div>
                        <div id="qrActiveState" class="d-none">
                            <div id="qrCanvas" class="bg-white p-2 border rounded mb-2"></div>
                            <div class="fw-bold text-danger fs-5" id="qrAmount">0 đ</div>
                            <div class="small text-muted">Số tiền quét QR trong chế độ kiểm thử</div>
                            <div class="small text-muted">Mã đơn: <strong id="qrOrderCode"></strong></div>
                            <div class="small mt-1 qr-waiting" id="qrStatusText">
                                <i class="fa-solid fa-spinner fa-spin me-1"></i>Đang chờ ngân hàng xác nhận...
                            </div>
                            <div class="small text-muted mt-1">Hết hạn sau <strong id="qrCountdown">10:00</strong></div>
                            <div class="d-flex gap-2 justify-content-center mt-3">
                                <a id="qrCheckoutUrl" class="btn btn-sm btn-outline-primary" target="_blank">
                                    <i class="fa-solid fa-up-right-from-square me-1"></i>Mở trang thanh toán
                                </a>
                                <button type="button" class="btn btn-sm btn-outline-danger" onclick="cancelQrPayment()">
                                    <i class="fa-solid fa-xmark me-1"></i>Hủy QR
                                </button>
                            </div>
                        </div>
                    </div>

                    <button class="btn btn-success w-100 checkout-btn" onclick="checkout()">
                        <i class="fa-solid fa-circle-check me-2"></i>THANH TOÁN
                    </button>
                </div>
            </div>
        </div><!-- /pos-wrapper -->
    </div>
</div>

<!-- ===== APP ALERT MODAL ===== -->
<div class="modal fade" id="appAlertModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow-lg">
            <div class="modal-header">
                <h5 class="modal-title fw-bold" id="appAlertTitle">Thông báo</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body" id="appAlertMessage"></div>
            <div class="modal-footer">
                <button type="button" class="btn btn-light border d-none" id="appAlertCancelBtn" data-bs-dismiss="modal">Không</button>
                <button type="button" class="btn btn-primary" id="appAlertOkBtn" data-bs-dismiss="modal">Đã hiểu</button>
            </div>
        </div>
    </div>
</div>

<!-- ===== SUCCESS MODAL ===== -->
<div class="modal fade" id="successModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow-lg">
            <div class="modal-body text-center py-5 px-4">
                <div class="mb-3">
                    <i class="fa-solid fa-circle-check text-success" style="font-size:4rem;"></i>
                </div>
                <h4 class="fw-bold mb-1">Thanh toán thành công!</h4>
                <p class="text-muted" id="successMsg"></p>
                <hr>
                <div class="d-flex justify-content-center gap-3">
                    <button class="btn btn-secondary px-4" data-bs-dismiss="modal">
                        <i class="fa-solid fa-xmark me-1"></i>Đóng
                    </button>
                    <a class="btn btn-primary px-4" id="printInvoiceBtn" target="_blank" href="#">
                        <i class="fa-solid fa-print me-1"></i>In hóa đơn
                    </a>
                </div>
            </div>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
<script src="<%= request.getContextPath() %>/assets/vendor/qrcode.min.js"></script>
<script>
// ---- Load Sidebar ----


// ============================================================
//  PRODUCT DATA (loaded from PostgreSQL through Java Web)
// ============================================================
let PRODUCTS = [];

// ============================================================
//  RENDER PRODUCTS
// ============================================================
function renderProductSkeleton(count = 12) {
    const grid = document.getElementById('productGrid');
    grid.innerHTML = Array.from({ length: count }).map(() => `
        <div class="product-skeleton" aria-hidden="true">
            <div class="skeleton-icon"></div>
            <div class="skeleton-line short"></div>
            <div class="skeleton-line full"></div>
            <div class="skeleton-line medium"></div>
            <div class="skeleton-line price"></div>
        </div>
    `).join('');
}

function renderProducts(list) {
    const grid = document.getElementById('productGrid');
    grid.innerHTML = list.map(p => {
        const safeName = escapeHtml(p.name);
        const safeCat = escapeHtml(p.cat);
        const outOfStock = p.stock <= 0;
        const stockBadge = outOfStock
            ? '<span class="badge bg-danger" style="font-size:10px;">Hết hàng</span>'
            : p.stock <= 10
                ? `<span class="badge bg-warning text-dark" style="font-size:10px;">Kho: ${p.stock}</span>`
                : `<span class="badge bg-success" style="font-size:10px;">Kho: ${p.stock}</span>`;
        return `
        <div class="product-card ${outOfStock ? 'out-of-stock' : ''}"
             data-id="${p.id}" data-cat="${safeCat}"
             data-name="${safeName.toLowerCase()}"
             onclick="${outOfStock ? '' : `addToCart('${p.id}')`}">
            <div class="icon-area bg-primary-subtle text-primary">
                <i class="fa-solid fa-box"></i>
            </div>
            <div class="card-info">
                <div class="cat-label">${safeCat}</div>
                <div class="prod-name" title="${safeName}">${safeName}</div>
                <div class="d-flex justify-content-between align-items-center">
                    <span class="prod-price">${fmt(p.price)}</span>
                    ${stockBadge}
                </div>
            </div>
        </div>`;
    }).join('');
}
async function loadProducts() {
    renderProductSkeleton();
    try {
        const response = await fetch('<%= request.getContextPath() %>/api/products/sale');
        if (!response.ok) throw new Error('Không tải được sản phẩm.');
        PRODUCTS = await response.json(); renderProducts(PRODUCTS);
        const categories = [...new Set(PRODUCTS.map(p => p.cat))];
        const select = document.getElementById('categoryFilter');
        select.innerHTML = '<option value="">Tất cả danh mục</option>' + categories.map(c => `<option value="${escapeHtml(c)}">${escapeHtml(c)}</option>`).join('');
    } catch (e) {
        document.getElementById('productGrid').innerHTML =
            `<div class="alert alert-danger product-load-error">${escapeHtml(e.message)}</div>`;
    }
}
loadProducts();

// ============================================================
//  CART STATE
// ============================================================
let cart = {}; // { id: { ...product, qty } }
let cartTotal = 0;       // tạm tính (subtotal trước giảm giá)
let appliedDiscountCode = '';
let appliedDiscountAmount = 0;
let pointsToRedeem = 0;
let pointsDiscountAmount = 0;
let grandTotal = 0;      // tổng tiền sau giảm giá
let paymentMethod = 'cash';
let selectedCustomer = null;
let csrfToken = null;
let qrPayment = null;
let qrPollTimer = null;
let qrCountdownTimer = null;
const STORE_NAME = '<%= ((com.retail.retailstoremanagement.model.AppUser) request.getSession().getAttribute("currentUser")).getStoreName().replace("\\", "\\\\").replace("'", "\\'") %>';

function storeAlert(message, title = STORE_NAME + ' thông báo') {
    document.getElementById('appAlertTitle').textContent = title;
    document.getElementById('appAlertMessage').textContent = message;
    document.getElementById('appAlertCancelBtn').classList.add('d-none');
    const okBtn = document.getElementById('appAlertOkBtn');
    okBtn.textContent = 'Đã hiểu';
    okBtn.onclick = null;
    bootstrap.Modal.getOrCreateInstance(document.getElementById('appAlertModal')).show();
}

function storeConfirm(message, onOk, title = STORE_NAME + ' xác nhận') {
    document.getElementById('appAlertTitle').textContent = title;
    document.getElementById('appAlertMessage').textContent = message;
    const cancelBtn = document.getElementById('appAlertCancelBtn');
    cancelBtn.classList.remove('d-none');
    const okBtn = document.getElementById('appAlertOkBtn');
    okBtn.textContent = 'Đồng ý';
    okBtn.onclick = () => setTimeout(onOk, 0);
    bootstrap.Modal.getOrCreateInstance(document.getElementById('appAlertModal')).show();
}

async function ensureSecurityContext() {
    if (csrfToken) return;
    const response = await fetch('<%= request.getContextPath() %>/api/session');
    if (!response.ok) throw new Error('Phiên đăng nhập đã hết hạn.');
    csrfToken = (await response.json()).csrfToken;
}
ensureSecurityContext().catch(() => {});

function onCustomerCodeInput(el) {
    el.value = el.value.toUpperCase().replace(/\s/g, '');
    selectedCustomer = null;
    pointsToRedeem = 0;
    pointsDiscountAmount = 0;
    el.classList.remove('is-valid', 'is-invalid');
    document.getElementById('customerFeedback').classList.add('d-none');
    document.getElementById('pointsPanel').classList.add('d-none');
    document.getElementById('pointsInput').value = '';
    updateTotals();
    if (document.getElementById('discountCodeInput').value.trim()) clearAppliedDiscount(false);
}

async function lookupCustomer() {
    const input = document.getElementById('customerCodeInput');
    const feedback = document.getElementById('customerFeedback');
    const code = input.value.trim().toUpperCase();
    selectedCustomer = null;
    input.classList.remove('is-valid', 'is-invalid');
    feedback.classList.remove('d-none', 'text-success', 'text-danger', 'text-muted');
    if (!code) {
        feedback.textContent = 'Để trống để thanh toán cho khách lẻ.';
        feedback.classList.add('text-muted');
        return true;
    }

    try {
        const response = await fetch('<%= request.getContextPath() %>/api/customers/lookup?code=' + encodeURIComponent(code));
        if (!response.ok) throw new Error('Lookup failed');
        const data = await response.json();
        if (!data.found) {
            input.classList.add('is-invalid');
            feedback.textContent = 'Không tìm thấy mã khách hàng này.';
            feedback.classList.add('text-danger');
            return false;
        }
        selectedCustomer = data;
        input.classList.add('is-valid');
        feedback.innerHTML =
            `<i class="fa-solid fa-circle-check me-1"></i>${data.name}`
            + ` · ${customerTypeLabel(data.type)}`
            + ` · ${data.points || 0} điểm khả dụng`
            + ` · ${data.lifetimePoints || 0} điểm tích lũy`;
        feedback.classList.add('text-success');
        document.getElementById('pointsPanel').classList.remove('d-none');
        document.getElementById('pointsHint').textContent =
            `${data.points || 0} điểm có thể dùng ngay · Tổng tích lũy ${data.lifetimePoints || 0} điểm · Quy đổi 1 điểm = 100đ.`;
        if (document.getElementById('discountCodeInput').value.trim()) clearAppliedDiscount(false);
        updateTotals();
        return true;
    } catch (error) {
        input.classList.add('is-invalid');
        feedback.textContent = 'Không thể kết nối dịch vụ khách hàng. Hãy chạy ứng dụng Java Web.';
        feedback.classList.add('text-danger');
        return false;
    }
}

function setPaymentMethod(method) {
    if (qrPayment && qrPayment.status === 'PENDING' && method !== 'qr') {
        storeAlert('Hãy hủy giao dịch QR đang chờ trước khi đổi phương thức thanh toán.');
        return;
    }
    paymentMethod = method;
    document.getElementById('cashMethodBtn').classList.toggle('active', method === 'cash');
    document.getElementById('qrMethodBtn').classList.toggle('active', method === 'qr');
    document.getElementById('cashPaymentPanel').classList.toggle('d-none', method !== 'cash');
    document.getElementById('qrPaymentPanel').classList.toggle('d-none', method !== 'qr');
    document.getElementById('cashInput').classList.remove('is-invalid');
}

function openQrPayment() {
    setPaymentMethod('qr');
    document.getElementById('qrPaymentPanel').scrollIntoView({ behavior: 'smooth', block: 'center' });
}

function addToCart(id) {
    if (qrPayment && qrPayment.status === 'PENDING') return;
    const p = PRODUCTS.find(x => x.id === id);
    if (!p || p.stock <= 0) return;
    if (cart[id]) {
        if (cart[id].qty >= p.stock) {
            storeAlert(`Không đủ hàng! Kho chỉ còn ${p.stock} sản phẩm.`);
            return;
        }
        cart[id].qty++;
    } else {
        cart[id] = { ...p, qty: 1 };
    }
    renderCart();
}

function changeQty(id, delta) {
    if (qrPayment && qrPayment.status === 'PENDING') return;
    if (!cart[id]) return;
    const newQty = cart[id].qty + delta;
    if (newQty <= 0) { delete cart[id]; }
    else if (newQty > cart[id].stock) {
        storeAlert(`Không đủ hàng! Kho chỉ còn ${cart[id].stock} sản phẩm.`);
        return;
    } else { cart[id].qty = newQty; }
    renderCart();
}

function removeItem(id) {
    if (qrPayment && qrPayment.status === 'PENDING') return;
    delete cart[id];
    renderCart();
}

function setQty(id, rawValue) {
    if (qrPayment && qrPayment.status === 'PENDING') return;
    if (!cart[id]) return;
    const newQty = parseInt(String(rawValue).replace(/\D/g, ''), 10) || 0;
    if (newQty <= 0) {
        delete cart[id];
    } else if (newQty > cart[id].stock) {
        storeAlert(`Không đủ hàng! Kho chỉ còn ${cart[id].stock} sản phẩm.`);
        cart[id].qty = cart[id].stock;
    } else {
        cart[id].qty = newQty;
    }
    renderCart();
}

function clearCart(force = false) {
    if (!force && qrPayment && qrPayment.status === 'PENDING') {
        storeAlert('Hãy hủy giao dịch QR đang chờ trước khi xóa giỏ hàng.');
        return;
    }
    cart = {};
    clearAppliedDiscount(true);
    renderCart();
    document.getElementById('cashInput').value = '';
    document.getElementById('cashInput').classList.remove('is-invalid');
    document.getElementById('changeAmount').textContent = '0 đ';
    selectedCustomer = null;
    pointsToRedeem = 0;
    pointsDiscountAmount = 0;
    document.getElementById('customerCodeInput').value = '';
    document.getElementById('customerCodeInput').classList.remove('is-valid', 'is-invalid');
    document.getElementById('customerFeedback').classList.add('d-none');
    document.getElementById('pointsPanel').classList.add('d-none');
    document.getElementById('pointsInput').value = '';
}

function renderCart() {
    const keys = Object.keys(cart);
    cartTotal = keys.reduce((s, id) => s + cart[id].price * cart[id].qty, 0);

    document.getElementById('cartEmpty').classList.toggle('d-none', keys.length > 0);
    document.getElementById('cartEmpty').classList.toggle('d-flex', keys.length === 0);
    document.getElementById('cartItems').classList.toggle('d-none', keys.length === 0);

    document.getElementById('cartItems').innerHTML = keys.map(id => {
        const item = cart[id];
        const sub = item.price * item.qty;
        const safeItemName = escapeHtml(item.name);
        return `
        <div class="cart-item">
            <div class="item-info">
                <div class="item-name" title="${safeItemName}">${safeItemName}</div>
                <div class="item-price">${fmt(item.price)} / cái</div>
            </div>
            <div class="qty-ctrl">
                <button onclick="changeQty('${id}',-1)">−</button>
                <input class="qty-input" inputmode="numeric" value="${item.qty}"
                       onfocus="this.select()"
                       onkeydown="if(event.key==='Enter'){this.blur()}"
                       onchange="setQty('${id}', this.value)">
                <button onclick="changeQty('${id}',1)">+</button>
            </div>
            <div class="item-subtotal">${fmt(sub)}</div>
            <button class="btn-remove-item" onclick="removeItem('${id}')">
                <i class="fa-solid fa-xmark"></i>
            </button>
        </div>`;
    }).join('');

    document.getElementById('cartCount').textContent = `${keys.length} mặt hàng`;
    document.getElementById('summaryItemCount').textContent = keys.length;
    document.getElementById('subtotalAmount').textContent = fmt(cartTotal);
    if (appliedDiscountCode) clearAppliedDiscount(false);
    updateTotals();
}

function onPointsInput(el) {
    let value = parseInt(el.value, 10) || 0;
    value = Math.max(0, value);
    if (!selectedCustomer) value = 0;
    const available = selectedCustomer ? Number(selectedCustomer.points || 0) : 0;
    const maxByAmount = Math.floor(Math.max(0, cartTotal - appliedDiscountAmount) / 100);
    value = Math.min(value, available, maxByAmount);
    pointsToRedeem = value;
    el.value = value || '';
    updateTotals();
}

function useMaxPoints() {
    if (!selectedCustomer) {
        storeAlert('Hãy nhập và kiểm tra mã khách hàng trước khi dùng điểm.');
        return;
    }
    const input = document.getElementById('pointsInput');
    input.value = Math.min(
        Number(selectedCustomer.points || 0),
        Math.floor(Math.max(0, cartTotal - appliedDiscountAmount) / 100)
    );
    onPointsInput(input);
}

// ============================================================
//  DISCOUNT CODE — backend kiểm tra điều kiện và tính giá trị
// ============================================================
function onDiscountCodeInput(el) {
    el.value = el.value.toUpperCase().replace(/[^A-Z0-9_-]/g, '');
    if (el.value !== appliedDiscountCode) clearAppliedDiscount(false);
}

function clearAppliedDiscount(clearInput) {
    appliedDiscountCode = '';
    appliedDiscountAmount = 0;
    const input = document.getElementById('discountCodeInput');
    input.classList.remove('is-valid', 'is-invalid');
    if (clearInput) input.value = '';
    document.getElementById('discountFeedback').classList.add('d-none');
    updateTotals();
}

async function applyDiscountCode() {
    const input = document.getElementById('discountCodeInput');
    const feedback = document.getElementById('discountFeedback');
    const code = input.value.trim().toUpperCase();
    input.classList.remove('is-valid', 'is-invalid');
    feedback.classList.remove('d-none', 'text-success', 'text-danger');
    if (!code) {
        clearAppliedDiscount(false);
        return;
    }
    if (cartTotal <= 0) {
        input.classList.add('is-invalid');
        feedback.textContent = 'Hãy thêm sản phẩm trước khi áp dụng mã.';
        feedback.classList.add('text-danger');
        return;
    }
    try {
        const params = new URLSearchParams({code, subtotal: cartTotal});
        const customerCode = document.getElementById('customerCodeInput').value.trim();
        if (customerCode) params.set('customerCode', customerCode);
        Object.values(cart).forEach(item => {
            params.append('productCode', item.id);
            params.append('lineTotal', String(item.price * item.qty));
        });
        const url = '<%= request.getContextPath() %>/api/discount-codes/validate?' + params.toString();
        const response = await fetch(url);
        const result = await response.json();
        if (!response.ok || !result.valid) throw new Error(result.message || 'Mã không hợp lệ.');
        appliedDiscountCode = result.code;
        appliedDiscountAmount = Number(result.discount) || 0;
        input.value = result.code;
        input.classList.add('is-valid');
        feedback.textContent = result.name + ' · Giảm ' + fmt(appliedDiscountAmount);
        feedback.classList.add('text-success');
        updateTotals();
    } catch (error) {
        appliedDiscountCode = '';
        appliedDiscountAmount = 0;
        input.classList.add('is-invalid');
        feedback.textContent = error.message;
        feedback.classList.add('text-danger');
        updateTotals();
    }
}

function updateTotals() {
    const afterDiscountCode = Math.max(0, cartTotal - appliedDiscountAmount);
    const maxPoints = selectedCustomer
        ? Math.min(Number(selectedCustomer.points || 0), Math.floor(afterDiscountCode / 100))
        : 0;
    if (pointsToRedeem > maxPoints) {
        pointsToRedeem = maxPoints;
        document.getElementById('pointsInput').value = pointsToRedeem || '';
    }
    pointsDiscountAmount = pointsToRedeem * 100;
    grandTotal = Math.max(0, afterDiscountCode - pointsDiscountAmount);
    document.getElementById('discountAmountDisplay').textContent =
        appliedDiscountAmount > 0 ? `- ${fmt(appliedDiscountAmount)}` : fmt(0);
    document.getElementById('pointsDiscountDisplay').textContent =
        pointsDiscountAmount > 0 ? `- ${fmt(pointsDiscountAmount)}` : fmt(0);
    document.getElementById('totalAmount').textContent = fmt(grandTotal);

    // Recalculate change against tổng tiền sau giảm giá
    const cashRaw = document.getElementById('cashInput').value.replace(/\D/g,'');
    const cash = parseInt(cashRaw) || 0;
    document.getElementById('changeAmount').textContent = cash >= grandTotal
        ? fmt(cash - grandTotal) : '0 đ';
}

// ============================================================
//  CASH INPUT — format as number, compute change
// ============================================================
function onCashInput(el) {
    const digits = el.value.replace(/\D/g,'');
    const n = parseInt(digits) || 0;
    el.value = n > 0 ? new Intl.NumberFormat('vi-VN').format(n) : '';
    el.classList.remove('is-invalid');
    document.getElementById('changeAmount').textContent =
        n >= grandTotal ? fmt(n - grandTotal) : '0 đ';
}

// ============================================================
//  CHECKOUT
// ============================================================
async function checkout() {
    if (Object.keys(cart).length === 0) {
        storeAlert('Giỏ hàng đang trống! Vui lòng chọn sản phẩm trước khi thanh toán.'); return;
    }
    try { await ensureSecurityContext(); } catch (error) { storeAlert(error.message); return; }
    const customerCode = document.getElementById('customerCodeInput').value.trim();
    if (customerCode && !selectedCustomer) {
        const found = await lookupCustomer();
        if (!found) return;
    }
    const cashRaw = document.getElementById('cashInput').value.replace(/\D/g,'');
    const cash = parseInt(cashRaw) || 0;
    const cashEl = document.getElementById('cashInput');
    if (paymentMethod === 'cash' && (!cashRaw || cash < grandTotal)) {
        cashEl.classList.add('is-invalid'); return;
    }
    cashEl.classList.remove('is-invalid');
    const enteredCode = document.getElementById('discountCodeInput').value.trim();
    if (enteredCode && enteredCode !== appliedDiscountCode) {
        storeAlert('Vui lòng nhấn Áp dụng để kiểm tra mã giảm giá.'); return;
    }
    await submitCheckout();
}

async function submitCheckout() {
    const customerCode = document.getElementById('customerCodeInput').value.trim();
    const cashRaw = document.getElementById('cashInput').value.replace(/\D/g,'');
    const cash = parseInt(cashRaw) || 0;
    const cashEl = document.getElementById('cashInput');
    const body = new URLSearchParams();
    Object.values(cart).forEach(item => { body.append('productCode', item.id); body.append('quantity', item.qty); });
    body.set('customerCode', customerCode); body.set('paymentMethod', paymentMethod.toUpperCase());
    body.set('csrfToken', csrfToken);
    if (appliedDiscountCode) body.set('discountCode', appliedDiscountCode);
    if (pointsToRedeem > 0) body.set('pointsToRedeem', pointsToRedeem);
    if (paymentMethod === 'cash') body.set('cashReceived', cash);
    const button = document.querySelector('.checkout-btn'); button.disabled = true;
    try {
        const response = await fetch('<%= request.getContextPath() %>/api/checkout', {method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'}, body});
        const result = await response.json();
        if (!response.ok || !result.success) throw new Error(result.message || 'Thanh toán thất bại.');
        if (result.pending) {
            await showQrPayment(result);
            return;
        }
        const discountLine = result.discount > 0 ? `<br>Giảm giá: <strong class="text-success">- ${fmt(result.discount)}</strong>` : '';
        const pointLine = result.pointsRedeemed > 0
            ? `<br>Dùng điểm: <strong class="text-success">${result.pointsRedeemed} điểm (- ${fmt(result.pointsDiscount)})</strong>`
            : '';
        const earnedLine = result.pointsEarned > 0
            ? `<br>Tích điểm: <strong class="text-primary">+${result.pointsEarned} điểm</strong>`
            : '';
        document.getElementById('successMsg').innerHTML = `Hóa đơn <strong>${result.code}</strong> đã được lưu.${discountLine}${pointLine}${earnedLine}<br>Tổng tiền: <strong class="text-danger">${fmt(result.total)}</strong>${result.change == null ? '' : `<br>Tiền thối: <strong class="text-success">${fmt(result.change)}</strong>`}`;
        document.getElementById('printInvoiceBtn').href =
            '<%= request.getContextPath() %>/invoices/print?id=' + result.invoiceId;
        bootstrap.Modal.getOrCreateInstance(document.getElementById('successModal')).show(); clearCart(); cashEl.value=''; await loadProducts();
    } catch (error) {
        storeAlert(error.message);
    } finally {
        button.disabled = false;
    }
}

async function showQrPayment(result) {
    qrPayment = {...result, status:'PENDING'};
    document.getElementById('qrReadyState').classList.add('d-none');
    document.getElementById('qrActiveState').classList.remove('d-none');
    document.getElementById('qrAmount').textContent = fmt(result.total);
    document.getElementById('qrOrderCode').textContent = result.orderCode;
    document.getElementById('qrCheckoutUrl').href = result.checkoutUrl;
    document.querySelector('.checkout-btn').classList.add('d-none');
    const qrContainer = document.getElementById('qrCanvas');
    qrContainer.innerHTML = '';
    if (typeof QRCode === 'undefined') {
        qrContainer.innerHTML =
            '<div class="alert alert-warning small mb-0">Không thể vẽ QR trên màn hình. '
            + '<a href="' + escapeHtml(result.checkoutUrl) + '" target="_blank">Mở trang thanh toán payOS</a>.</div>';
    } else {
        new QRCode(qrContainer, {
            text: result.qrCode,
            width: 194,
            height: 194,
            colorDark: '#000000',
            colorLight: '#ffffff',
            correctLevel: QRCode.CorrectLevel.M
        });
    }
    startQrCountdown(result.expiresAt);
    stopQrPolling();
    qrPollTimer = setInterval(checkQrStatus, 2500);
}

function startQrCountdown(expiresAt) {
    clearInterval(qrCountdownTimer);
    const render = () => {
        const remaining = Math.max(0, new Date(expiresAt).getTime() - Date.now());
        const seconds = Math.floor(remaining / 1000);
        document.getElementById('qrCountdown').textContent =
            String(Math.floor(seconds / 60)).padStart(2,'0') + ':'
            + String(seconds % 60).padStart(2,'0');
        if (remaining <= 0) checkQrStatus();
    };
    render();
    qrCountdownTimer = setInterval(render, 1000);
}

async function checkQrStatus() {
    if (!qrPayment || qrPayment.status !== 'PENDING') return;
    try {
        const response = await fetch('<%= request.getContextPath() %>/api/payments/status?invoiceId='
            + encodeURIComponent(qrPayment.invoiceId), {cache:'no-store'});
        const result = await response.json();
        if (!response.ok || !result.success) return;
        qrPayment.status = result.status;
        if (result.status === 'PAID') {
            finishQrPayment(result);
        } else if (['CANCELLED','EXPIRED','FAILED','REVIEW'].includes(result.status)) {
            stopQrPolling();
            document.getElementById('qrStatusText').classList.remove('qr-waiting');
            document.getElementById('qrStatusText').innerHTML =
                `<span class="text-danger"><i class="fa-solid fa-circle-xmark me-1"></i>${
                    escapeHtml(result.message || qrStatusLabel(result.status))
                }</span>`;
            document.querySelector('.checkout-btn').classList.remove('d-none');
            await loadProducts();
        }
    } catch (error) {
        // Giữ polling; mất mạng tạm thời không được xem là thanh toán thất bại.
    }
}

function finishQrPayment(result) {
    stopQrPolling();
    document.getElementById('qrStatusText').classList.remove('qr-waiting');
    document.getElementById('qrStatusText').innerHTML =
        '<span class="text-success"><i class="fa-solid fa-circle-check me-1"></i>Ngân hàng đã xác nhận thanh toán.</span>';
    const invoiceTotal = result.invoiceTotal ?? result.total;
    const qrPaidLine = result.invoiceTotal && Number(result.invoiceTotal) !== Number(result.total)
        ? `<br>Số tiền QR test: <strong class="text-primary">${fmt(result.total)}</strong>`
        : '';
    document.getElementById('successMsg').innerHTML =
        `Hóa đơn <strong>${escapeHtml(result.code)}</strong> đã thanh toán qua QR.<br>`
        + `Tổng hóa đơn: <strong class="text-danger">${fmt(invoiceTotal)}</strong>${qrPaidLine}`;
    document.getElementById('printInvoiceBtn').href =
        '<%= request.getContextPath() %>/invoices/print?id=' + result.invoiceId;
    bootstrap.Modal.getOrCreateInstance(document.getElementById('successModal')).show();
    clearCart(true);
    resetQrPanel();
    loadProducts();
}

async function cancelQrPayment() {
    if (!qrPayment || qrPayment.status !== 'PENDING') {
        resetQrPanel();
        return;
    }
    storeConfirm('Hủy giao dịch QR này và hoàn lại hàng đang giữ?', async () => {
    await ensureSecurityContext();
    const body = new URLSearchParams({
        invoiceId: qrPayment.invoiceId,
        csrfToken
    });
    try {
        const response = await fetch('<%= request.getContextPath() %>/api/payments/cancel', {
            method:'POST',
            headers:{'Content-Type':'application/x-www-form-urlencoded;charset=UTF-8'},
            body
        });
        const result = await response.json();
        if (!response.ok || !result.success) throw new Error(result.message || 'Không thể hủy QR.');
        resetQrPanel();
        await loadProducts();
    } catch (error) {
        storeAlert(error.message);
    }
    });
}

function resetQrPanel() {
    stopQrPolling();
    qrPayment = null;
    document.getElementById('qrReadyState').classList.remove('d-none');
    document.getElementById('qrActiveState').classList.add('d-none');
    document.getElementById('qrCanvas').innerHTML = '';
    document.getElementById('qrStatusText').classList.add('qr-waiting');
    document.getElementById('qrStatusText').innerHTML =
        '<i class="fa-solid fa-spinner fa-spin me-1"></i>Đang chờ ngân hàng xác nhận...';
    document.querySelector('.checkout-btn').classList.remove('d-none');
}

function stopQrPolling() {
    clearInterval(qrPollTimer);
    clearInterval(qrCountdownTimer);
    qrPollTimer = null;
    qrCountdownTimer = null;
}

function qrStatusLabel(status) {
    return {
        CANCELLED:'Giao dịch đã hủy.',
        EXPIRED:'Mã QR đã hết hạn.',
        FAILED:'Không thể tạo hoặc xử lý giao dịch.',
        REVIEW:'Giao dịch cần được quản trị viên kiểm tra.'
    }[status] || 'Giao dịch không thành công.';
}

// ============================================================
//  FILTER PRODUCTS
// ============================================================
function filterProducts() {
    const q   = document.getElementById('searchInput').value.toLowerCase();
    const cat = document.getElementById('categoryFilter').value;
    document.querySelectorAll('#productGrid .product-card').forEach(card => {
        const matchQ   = card.dataset.name.includes(q) || card.dataset.id.toLowerCase().includes(q);
        const matchCat = !cat || card.dataset.cat === cat;
        card.style.display = (matchQ && matchCat) ? '' : 'none';
    });
}

// ============================================================
//  HELPERS
// ============================================================
function fmt(n) { return new Intl.NumberFormat('vi-VN').format(n) + ' đ'; }
function escapeHtml(value) { const el=document.createElement('div'); el.textContent=value??''; return el.innerHTML; }
function customerTypeLabel(type) {
    return {REGULAR:'Khách thường', LOYAL:'Thân thiết', VIP:'VIP'}[type] || 'Khách';
}
</script>
</body>
</html>
