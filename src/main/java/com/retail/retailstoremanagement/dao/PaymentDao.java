package com.retail.retailstoremanagement.dao;

import com.retail.retailstoremanagement.model.PaymentStatus;
import com.retail.retailstoremanagement.model.PaymentTransaction;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Map;

public interface PaymentDao {
    PaymentTransaction createPending(Map<String, Integer> items, String customerCode,
                                     String discountCode, Long cashierId,
                                     OffsetDateTime expiresAt) throws SQLException;
    PaymentTransaction attachPayOs(long orderCode, String paymentLinkId,
                                   String checkoutUrl, String qrCode) throws SQLException;
    PaymentTransaction findByInvoice(long invoiceId) throws SQLException;
    Long findStoreIdByOrderCode(long orderCode) throws SQLException;
    PaymentTransaction markPaid(long orderCode, BigDecimal amount,
                                String providerReference, String rawWebhook)
            throws SQLException;
    PaymentTransaction release(long orderCode, PaymentStatus status,
                               String reason) throws SQLException;
}
