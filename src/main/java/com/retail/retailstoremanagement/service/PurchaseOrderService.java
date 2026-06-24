package com.retail.retailstoremanagement.service;

import com.retail.retailstoremanagement.dao.PurchaseOrderDao;
import com.retail.retailstoremanagement.dao.impl.JdbcPurchaseOrderDao;
import com.retail.retailstoremanagement.model.PurchaseOrder;
import com.retail.retailstoremanagement.model.PurchaseOrderDetail;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PurchaseOrderService {
    private final PurchaseOrderDao dao;

    public PurchaseOrderService() {
        this(new JdbcPurchaseOrderDao());
    }

    public PurchaseOrderService(PurchaseOrderDao dao) {
        this.dao = dao;
    }

    public List<PurchaseOrder> findAll(String keyword, String status, int page)
            throws SQLException {
        int safePage = Math.max(1, page);
        return dao.findAll(keyword, status, 30, (safePage - 1) * 30);
    }

    public PurchaseOrder findById(long id) throws SQLException {
        PurchaseOrder order = dao.findById(id);
        if (order == null) throw new ValidationException("Không tìm thấy phiếu nhập.");
        return order;
    }

    public PurchaseOrder createDraft(long supplierId, String note, Long userId,
                                     List<PurchaseOrderDetail> details) throws SQLException {
        if (supplierId <= 0) throw new ValidationException("Vui lòng chọn nhà cung cấp.");
        if (details == null || details.isEmpty()) {
            throw new ValidationException("Phiếu nhập cần ít nhất một sản phẩm.");
        }
        if (note != null && note.length() > 500) {
            throw new ValidationException("Ghi chú không được vượt quá 500 ký tự.");
        }
        Set<Long> productIds = new HashSet<>();
        for (PurchaseOrderDetail detail : details) {
            if (detail.getProductId() == null || detail.getProductId() <= 0) {
                throw new ValidationException("Sản phẩm không hợp lệ.");
            }
            if (!productIds.add(detail.getProductId())) {
                throw new ValidationException("Một sản phẩm chỉ được xuất hiện một lần trong phiếu.");
            }
            if (detail.getQuantity() <= 0) {
                throw new ValidationException("Số lượng nhập phải lớn hơn 0.");
            }
            if (detail.getUnitCost() == null
                    || detail.getUnitCost().compareTo(BigDecimal.ZERO) < 0) {
                throw new ValidationException("Giá nhập không được âm.");
            }
        }
        return dao.createDraft(supplierId, note == null ? "" : note.trim(), userId, details);
    }

    public void complete(long id, Long userId) throws SQLException {
        dao.complete(id, userId);
    }

    public void cancel(long id, Long userId) throws SQLException {
        dao.cancel(id, userId);
    }
}
