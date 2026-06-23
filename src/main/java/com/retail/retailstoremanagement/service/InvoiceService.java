package com.retail.retailstoremanagement.service;

import com.retail.retailstoremanagement.dao.InvoiceDao;
import com.retail.retailstoremanagement.model.DiscountType;
import com.retail.retailstoremanagement.model.Invoice;
import com.retail.retailstoremanagement.model.PaymentMethod;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class InvoiceService {
    private final InvoiceDao dao;
    public InvoiceService(InvoiceDao dao) { this.dao = dao; }

    /** Giữ lại cho code/test cũ gọi không kèm giảm giá — mặc định không giảm giá. */
    public Invoice checkout(Map<String,Integer> items, String customerCode, String payment,
                            BigDecimal cashReceived, Long cashierId) throws SQLException {
        return checkout(items, customerCode, payment, cashReceived, null, null, cashierId);
    }

    public Invoice checkout(Map<String,Integer> items, String customerCode, String payment,
                            BigDecimal cashReceived, String discountTypeRaw, BigDecimal discountValue,
                            Long cashierId) throws SQLException {
        if (items == null || items.isEmpty()) throw new ValidationException("Giỏ hàng đang trống.");
        for (Integer quantity : items.values()) {
            if (quantity == null || quantity <= 0) throw new ValidationException("Số lượng sản phẩm không hợp lệ.");
        }
        PaymentMethod method;
        try { method = PaymentMethod.valueOf(payment.toUpperCase()); }
        catch (Exception e) { throw new ValidationException("Phương thức thanh toán không hợp lệ."); }

        DiscountType discountType = DiscountType.AMOUNT;
        if (discountTypeRaw != null && !discountTypeRaw.isBlank()) {
            try { discountType = DiscountType.valueOf(discountTypeRaw.trim().toUpperCase()); }
            catch (Exception e) { throw new ValidationException("Loại giảm giá không hợp lệ."); }
        }
        BigDecimal value = discountValue == null ? BigDecimal.ZERO : discountValue;
        if (value.compareTo(BigDecimal.ZERO) < 0) throw new ValidationException("Giá trị giảm giá không được âm.");
        if (discountType == DiscountType.PERCENT && value.compareTo(BigDecimal.valueOf(100)) > 0)
            throw new ValidationException("Phần trăm giảm giá không được vượt quá 100%.");

        return dao.checkout(items, emptyToNull(customerCode), method, cashReceived, discountType, value, cashierId);
    }

    public List<Invoice> findAll(String keyword, String status, int pageSize, int page) throws SQLException {
        return dao.findAll(keyword, status, pageSize, Math.max(0, page - 1) * pageSize);
    }
    public Invoice findById(long id) throws SQLException { return dao.findById(id); }
    public void cancel(long id, Long userId) throws SQLException { dao.cancel(id, userId); }
    private String emptyToNull(String value) { return value == null || value.trim().isEmpty() ? null : value.trim().toUpperCase(); }
}
