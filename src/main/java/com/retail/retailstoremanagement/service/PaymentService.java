package com.retail.retailstoremanagement.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.retail.retailstoremanagement.dao.PaymentDao;
import com.retail.retailstoremanagement.dao.impl.JdbcPaymentDao;
import com.retail.retailstoremanagement.model.AppUser;
import com.retail.retailstoremanagement.model.PaymentStatus;
import com.retail.retailstoremanagement.model.PaymentTransaction;
import com.retail.retailstoremanagement.model.Store;
import com.retail.retailstoremanagement.util.TenantContext;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Map;

public class PaymentService {
    private static final int QR_EXPIRY_MINUTES = 10;
    private final PaymentDao dao;
    private final PayOsClient payOs;
    private final StoreService storeService = new StoreService();

    public PaymentService() {
        this(new JdbcPaymentDao(), null);
    }

    public PaymentService(PaymentDao dao, PayOsClient payOs) {
        this.dao = dao;
        this.payOs = payOs;
    }

    public PaymentTransaction startQr(Map<String, Integer> items, String customerCode,
                                      String discountCode, AppUser cashier) throws Exception {
        validate(items, cashier);
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(QR_EXPIRY_MINUTES);
        PaymentTransaction payment = dao.createPending(
                items, emptyToNull(customerCode), emptyToNull(discountCode),
                cashier.getId(), expiresAt);
        try {
            long amount = payment.getAmount().longValueExact();
            PayOsClient.PaymentLink link = payOs().createPayment(
                    payment.getOrderCode(), amount,
                    description(payment.getOrderCode()), expiresAt);
            return dao.attachPayOs(
                    payment.getOrderCode(), link.getPaymentLinkId(),
                    link.getCheckoutUrl(), link.getQrCode());
        } catch (Exception exception) {
            dao.release(payment.getOrderCode(), PaymentStatus.FAILED,
                    "Không tạo được mã QR payOS: " + safeMessage(exception));
            throw exception;
        }
    }

    public PaymentTransaction getStatus(long invoiceId) throws Exception {
        PaymentTransaction payment = dao.findByInvoice(invoiceId);
        if (payment == null) throw new ValidationException("Không tìm thấy giao dịch QR.");
        if (payment.getStatus() == PaymentStatus.PENDING
                && payment.getExpiresAt().isBefore(OffsetDateTime.now())) {
            try {
                payOs().cancelPayment(payment.getOrderCode(), "Giao dịch hết hạn");
            } catch (Exception ignored) {
                // Local expiration still releases the held stock. A late signed webhook
                // is moved to REVIEW instead of being silently marked paid.
            }
            payment = dao.release(payment.getOrderCode(), PaymentStatus.EXPIRED,
                    "Mã QR đã hết hạn.");
        }
        return payment;
    }

    public PaymentTransaction cancel(long invoiceId) throws Exception {
        PaymentTransaction payment = dao.findByInvoice(invoiceId);
        if (payment == null) throw new ValidationException("Không tìm thấy giao dịch QR.");
        if (payment.getStatus() != PaymentStatus.PENDING) return payment;
        payOs().cancelPayment(payment.getOrderCode(), "Thu ngân hủy thanh toán");
        return dao.release(payment.getOrderCode(), PaymentStatus.CANCELLED,
                "Thu ngân hủy thanh toán QR.");
    }

    public WebhookResult processWebhook(JsonNode root, String rawBody) throws Exception {
        JsonNode data = root.path("data");
        String signature = root.path("signature").asText(null);
        long orderCode = data.path("orderCode").asLong(0);
        PayOsClient verifier = payOsForWebhook(orderCode);
        if (!verifier.verifyWebhook(data, signature)) {
            throw new SecurityException("Chữ ký webhook payOS không hợp lệ.");
        }
        if (orderCode <= 0) return WebhookResult.ignored("Webhook kiểm tra kết nối.");
        Long storeId = dao.findStoreIdByOrderCode(orderCode);
        if (storeId == null) return WebhookResult.ignored("Không có giao dịch nội bộ tương ứng.");

        Long previous = TenantContext.getStoreId();
        TenantContext.setStoreId(storeId);
        try {
            boolean success = "00".equals(data.path("code").asText())
                    || data.path("success").asBoolean(false);
            if (!success) return WebhookResult.ignored("Webhook không phải giao dịch thành công.");
            BigDecimal amount = data.path("amount").decimalValue();
            String reference = data.path("reference").asText(null);
            PaymentTransaction payment = dao.markPaid(
                    orderCode, amount, reference, rawBody);
            return WebhookResult.processed(payment);
        } finally {
            if (previous == null) TenantContext.clear();
            else TenantContext.setStoreId(previous);
        }
    }

    private void validate(Map<String, Integer> items, AppUser cashier) {
        if (items == null || items.isEmpty()) {
            throw new ValidationException("Giỏ hàng đang trống.");
        }
        for (Integer quantity : items.values()) {
            if (quantity == null || quantity <= 0) {
                throw new ValidationException("Số lượng sản phẩm không hợp lệ.");
            }
        }
        if (cashier == null) throw new ValidationException("Không xác định được thu ngân.");
    }

    private String description(long orderCode) {
        String value = "HD" + orderCode;
        return value.length() <= 9 ? value : value.substring(value.length() - 9);
    }

    private String emptyToNull(String value) {
        return value == null || value.trim().isEmpty()
                ? null : value.trim().toUpperCase();
    }

    private String safeMessage(Exception exception) {
        return exception.getMessage() == null
                ? exception.getClass().getSimpleName() : exception.getMessage();
    }

    private PayOsClient payOs() throws SQLException {
        if (payOs != null) return payOs;
        Store store = storeService.findCurrent();
        if (store.isPayOsEnabled() && store.hasPayOsCredentials()) {
            return new PayOsClient(store.getPayOsClientId(),
                    store.getPayOsApiKey(), store.getPayOsChecksumKey());
        }
        return new PayOsClient();
    }

    private PayOsClient payOsForWebhook(long orderCode) throws SQLException {
        if (payOs != null) return payOs;
        if (orderCode > 0) {
            Long storeId = dao.findStoreIdByOrderCode(orderCode);
            if (storeId != null) {
                Store store = storeService.findPayOsForStoreId(storeId);
                if (store != null && store.isPayOsEnabled()
                        && store.hasPayOsCredentials()) {
                    return new PayOsClient(store.getPayOsClientId(),
                            store.getPayOsApiKey(), store.getPayOsChecksumKey());
                }
            }
        }
        return new PayOsClient();
    }

    public static final class WebhookResult {
        private final boolean processed;
        private final String message;
        private final PaymentTransaction payment;

        private WebhookResult(boolean processed, String message,
                              PaymentTransaction payment) {
            this.processed = processed;
            this.message = message;
            this.payment = payment;
        }

        public static WebhookResult processed(PaymentTransaction payment) {
            return new WebhookResult(true, "Đã xử lý.", payment);
        }

        public static WebhookResult ignored(String message) {
            return new WebhookResult(false, message, null);
        }

        public boolean isProcessed() { return processed; }
        public String getMessage() { return message; }
        public PaymentTransaction getPayment() { return payment; }
    }
}
