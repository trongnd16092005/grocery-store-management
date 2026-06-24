package com.retail.retailstoremanagement.service;

import com.retail.retailstoremanagement.dao.DiscountCodeDao;
import com.retail.retailstoremanagement.dao.impl.JdbcDiscountCodeDao;
import com.retail.retailstoremanagement.model.DiscountCode;
import com.retail.retailstoremanagement.model.DiscountType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;

public class DiscountCodeService {
    private final DiscountCodeDao dao;

    public DiscountCodeService() {
        this(new JdbcDiscountCodeDao());
    }

    public DiscountCodeService(DiscountCodeDao dao) {
        this.dao = dao;
    }

    public List<DiscountCode> findAll(boolean includeInactive) throws SQLException {
        return dao.findAll(includeInactive);
    }

    public DiscountCode find(long id) throws SQLException {
        return dao.findById(id)
                .orElseThrow(() -> new ValidationException("Không tìm thấy mã giảm giá."));
    }

    public DiscountCode save(DiscountCode code) throws SQLException {
        normalizeAndValidate(code);
        try {
            if (code.getId() == null) return dao.insert(code);
            if (!dao.update(code)) throw new ValidationException("Không tìm thấy mã cần sửa.");
            return code;
        } catch (SQLException exception) {
            if ("23505".equals(exception.getSQLState())) {
                throw new ValidationException("Mã giảm giá đã tồn tại.");
            }
            throw exception;
        }
    }

    public void setActive(long id, boolean active) throws SQLException {
        if (!dao.setActive(id, active)) {
            throw new ValidationException("Không thể cập nhật trạng thái mã giảm giá.");
        }
    }

    public DiscountPreview preview(String rawCode, BigDecimal subtotal) throws SQLException {
        if (rawCode == null || rawCode.isBlank()) {
            return new DiscountPreview("", BigDecimal.ZERO, subtotal, "Không sử dụng mã.");
        }
        DiscountCode code = dao.findByCode(rawCode.trim())
                .orElseThrow(() -> new ValidationException("Mã giảm giá không tồn tại."));
        BigDecimal discount = calculateValidDiscount(code, subtotal, OffsetDateTime.now());
        return new DiscountPreview(code.getCode(), discount,
                subtotal.subtract(discount), code.getName());
    }

    public BigDecimal calculateValidDiscount(DiscountCode code, BigDecimal subtotal,
                                             OffsetDateTime now) {
        if (!code.isActive()) throw new ValidationException("Mã giảm giá đã bị khóa.");
        if (code.getStartsAt() != null && now.isBefore(code.getStartsAt())) {
            throw new ValidationException("Mã giảm giá chưa đến thời gian sử dụng.");
        }
        if (code.getEndsAt() != null && !now.isBefore(code.getEndsAt())) {
            throw new ValidationException("Mã giảm giá đã hết hạn.");
        }
        if (code.getUsageLimit() != null && code.getUsedCount() >= code.getUsageLimit()) {
            throw new ValidationException("Mã giảm giá đã hết lượt sử dụng.");
        }
        if (subtotal.compareTo(code.getMinimumOrder()) < 0) {
            throw new ValidationException("Đơn hàng cần tối thiểu "
                    + code.getMinimumOrder().stripTrailingZeros().toPlainString() + " đ.");
        }
        BigDecimal discount = code.getDiscountType() == DiscountType.PERCENT
                ? subtotal.multiply(code.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : code.getDiscountValue();
        if (code.getMaximumDiscount() != null
                && discount.compareTo(code.getMaximumDiscount()) > 0) {
            discount = code.getMaximumDiscount();
        }
        return discount.min(subtotal).max(BigDecimal.ZERO);
    }

    private void normalizeAndValidate(DiscountCode code) {
        code.setCode(code.getCode() == null ? "" : code.getCode().trim().toUpperCase());
        code.setName(code.getName() == null ? "" : code.getName().trim());
        if (!code.getCode().matches("[A-Z0-9_-]{3,40}")) {
            throw new ValidationException("Mã cần 3–40 ký tự A-Z, 0-9, gạch ngang hoặc gạch dưới.");
        }
        if (code.getName().length() < 2 || code.getName().length() > 150) {
            throw new ValidationException("Tên chương trình cần từ 2 đến 150 ký tự.");
        }
        if (code.getDiscountType() == null || code.getDiscountValue() == null
                || code.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Giá trị giảm phải lớn hơn 0.");
        }
        if (code.getDiscountType() == DiscountType.PERCENT
                && code.getDiscountValue().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new ValidationException("Phần trăm giảm không được vượt quá 100%.");
        }
        if (code.getMinimumOrder() == null) code.setMinimumOrder(BigDecimal.ZERO);
        if (code.getMinimumOrder().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Đơn tối thiểu không được âm.");
        }
        if (code.getMaximumDiscount() != null
                && code.getMaximumDiscount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Mức giảm tối đa phải lớn hơn 0.");
        }
        if (code.getUsageLimit() != null && code.getUsageLimit() <= 0) {
            throw new ValidationException("Giới hạn lượt dùng phải lớn hơn 0.");
        }
        if (code.getStartsAt() != null && code.getEndsAt() != null
                && !code.getStartsAt().isBefore(code.getEndsAt())) {
            throw new ValidationException("Thời gian kết thúc phải sau thời gian bắt đầu.");
        }
    }

    public static final class DiscountPreview {
        private final String code;
        private final BigDecimal discount;
        private final BigDecimal total;
        private final String name;

        public DiscountPreview(String code, BigDecimal discount, BigDecimal total, String name) {
            this.code = code;
            this.discount = discount;
            this.total = total;
            this.name = name;
        }

        public String getCode() { return code; }
        public BigDecimal getDiscount() { return discount; }
        public BigDecimal getTotal() { return total; }
        public String getName() { return name; }
    }
}
