package com.retail.retailstoremanagement.controller;

import com.retail.retailstoremanagement.model.DiscountCode;
import com.retail.retailstoremanagement.model.DiscountType;
import com.retail.retailstoremanagement.service.DiscountCodeService;
import com.retail.retailstoremanagement.util.RequestUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@WebServlet("/discount-codes")
public class DiscountCodeServlet extends HttpServlet {
    private final DiscountCodeService service = new DiscountCodeService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            boolean includeInactive = "true".equals(request.getParameter("includeInactive"));
            request.setAttribute("codes", service.findAll(includeInactive));
            request.setAttribute("includeInactive", includeInactive);
            Long editId = RequestUtils.optionalLong(request, "editId");
            if (editId != null) {
                DiscountCode editingCode = service.find(editId);
                request.setAttribute("editingCode", editingCode);
                DateTimeFormatter formatter =
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
                if (editingCode.getStartsAt() != null) {
                    request.setAttribute("editingStartsAt",
                            editingCode.getStartsAt().atZoneSameInstant(
                                    ZoneId.of("Asia/Ho_Chi_Minh")).format(formatter));
                }
                if (editingCode.getEndsAt() != null) {
                    request.setAttribute("editingEndsAt",
                            editingCode.getEndsAt().atZoneSameInstant(
                                    ZoneId.of("Asia/Ho_Chi_Minh")).format(formatter));
                }
            }
            request.setAttribute("flashSuccess",
                    RequestUtils.consumeFlash(request, "flashSuccess"));
            request.setAttribute("flashError",
                    RequestUtils.consumeFlash(request, "flashError"));
            request.getRequestDispatcher("/WEB-INF/views/discount-codes.jsp")
                    .forward(request, response);
        } catch (Exception exception) {
            throw new ServletException(exception);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            String action = RequestUtils.text(request, "action");
            if ("lock".equals(action) || "unlock".equals(action)) {
                service.setActive(RequestUtils.requiredLong(request, "id"),
                        "unlock".equals(action));
                RequestUtils.flash(request, "flashSuccess", "Đã cập nhật trạng thái mã.");
            } else {
                DiscountCode code = new DiscountCode();
                code.setId(RequestUtils.optionalLong(request, "id"));
                code.setCode(RequestUtils.text(request, "code"));
                code.setName(RequestUtils.text(request, "name"));
                code.setDiscountType(DiscountType.valueOf(
                        RequestUtils.text(request, "discountType")));
                code.setDiscountValue(RequestUtils.decimal(request, "discountValue"));
                code.setMinimumOrder(RequestUtils.decimal(request, "minimumOrder"));
                code.setMaximumDiscount(optionalDecimal(request, "maximumDiscount"));
                code.setUsageLimit(optionalInteger(request, "usageLimit"));
                code.setStartsAt(optionalDateTime(request, "startsAt"));
                code.setEndsAt(optionalDateTime(request, "endsAt"));
                code.setCustomerTypeScope(RequestUtils.text(request, "customerTypeScope"));
                code.setProductCode(RequestUtils.text(request, "productCode"));
                service.save(code);
                RequestUtils.flash(request, "flashSuccess", "Đã lưu mã giảm giá.");
            }
        } catch (Exception exception) {
            String message = exception.getMessage();
            RequestUtils.flash(request, "flashError",
                    message == null ? "Không thể lưu mã giảm giá." : message);
        }
        response.sendRedirect(request.getContextPath() + "/discount-codes");
    }

    private BigDecimal optionalDecimal(HttpServletRequest request, String name) {
        String value = RequestUtils.text(request, name);
        return value.isEmpty() ? null : new BigDecimal(value);
    }

    private Integer optionalInteger(HttpServletRequest request, String name) {
        String value = RequestUtils.text(request, name);
        return value.isEmpty() ? null : Integer.valueOf(value);
    }

    private java.time.OffsetDateTime optionalDateTime(HttpServletRequest request, String name) {
        String value = RequestUtils.text(request, name);
        if (value.isEmpty()) return null;
        return LocalDateTime.parse(value)
                .atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                .toOffsetDateTime();
    }
}
