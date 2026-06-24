package com.retail.retailstoremanagement.service;

import com.retail.retailstoremanagement.dao.impl.JdbcInvoiceDao;
import com.retail.retailstoremanagement.dao.impl.JdbcDiscountCodeDao;
import com.retail.retailstoremanagement.dao.impl.JdbcProductDao;
import com.retail.retailstoremanagement.dao.impl.JdbcPurchaseOrderDao;
import com.retail.retailstoremanagement.dao.impl.JdbcSupplierDao;
import com.retail.retailstoremanagement.dao.impl.JdbcUserDao;
import com.retail.retailstoremanagement.model.AppUser;
import com.retail.retailstoremanagement.model.DiscountCode;
import com.retail.retailstoremanagement.model.DiscountType;
import com.retail.retailstoremanagement.model.Invoice;
import com.retail.retailstoremanagement.model.PaymentMethod;
import com.retail.retailstoremanagement.model.Product;
import com.retail.retailstoremanagement.model.PurchaseOrder;
import com.retail.retailstoremanagement.model.PurchaseOrderDetail;
import com.retail.retailstoremanagement.model.Supplier;
import com.retail.retailstoremanagement.model.UserRole;
import com.retail.retailstoremanagement.util.DatabaseConnection;
import com.retail.retailstoremanagement.util.TestTenantContext;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Manual integration test for multi-item purchase orders and discount approval.
 * Every record created by this test is removed before exit.
 */
public final class BusinessWorkflowSmokeTest {
    private BusinessWorkflowSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        TestTenantContext.activateDefaultStore();
        JdbcProductDao productDao = new JdbcProductDao();
        JdbcPurchaseOrderDao purchaseOrderDao = new JdbcPurchaseOrderDao();
        JdbcInvoiceDao invoiceDao = new JdbcInvoiceDao();
        Supplier supplier = new JdbcSupplierDao().findAll().get(0);
        Product product = productDao.findAll().stream()
                .filter(item -> item.getStockQuantity() >= 2)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Need a product with stock."));
        AppUser admin = new JdbcUserDao().findAll().stream()
                .filter(user -> user.getRole() == UserRole.ADMIN && user.isActive())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Need an active admin."));

        int originalStock = product.getStockQuantity();
        PurchaseOrder order = null;
        Invoice invoice = null;
        DiscountCode discountCode = null;
        try {
            PurchaseOrderDetail detail = new PurchaseOrderDetail();
            detail.setProductId(product.getId());
            detail.setQuantity(2);
            detail.setUnitCost(product.getCostPrice());
            order = purchaseOrderDao.createDraft(
                    supplier.getId(), "__WORKFLOW_SMOKE__", admin.getId(), List.of(detail));
            purchaseOrderDao.complete(order.getId(), admin.getId());
            if (productDao.findById(product.getId()).orElseThrow().getStockQuantity()
                    != originalStock + 2) {
                throw new IllegalStateException("Completing purchase order did not add stock.");
            }
            purchaseOrderDao.cancel(order.getId(), admin.getId());
            if (productDao.findById(product.getId()).orElseThrow().getStockQuantity()
                    != originalStock) {
                throw new IllegalStateException("Cancelling purchase order did not restore stock.");
            }

            discountCode = new DiscountCode();
            discountCode.setCode("SMOKE" + System.currentTimeMillis());
            discountCode.setName("__WORKFLOW_SMOKE__");
            discountCode.setDiscountType(DiscountType.PERCENT);
            discountCode.setDiscountValue(BigDecimal.valueOf(20));
            discountCode.setMinimumOrder(BigDecimal.ZERO);
            discountCode.setUsageLimit(1);
            discountCode = new JdbcDiscountCodeDao().insert(discountCode);

            invoice = invoiceDao.checkout(
                    Map.of(product.getCode(), 1), null, PaymentMethod.CASH,
                    product.getSellingPrice(), discountCode.getCode(), admin.getId()
            );
            if (!discountCode.getCode().equals(invoice.getDiscountCode())
                    || invoice.getDiscountAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("Discount code was not persisted.");
            }
            try {
                invoiceDao.checkout(
                        Map.of(product.getCode(), 1), null, PaymentMethod.CASH,
                        product.getSellingPrice(), discountCode.getCode(), admin.getId()
                );
                throw new IllegalStateException("Usage limit was bypassed.");
            } catch (SQLException expected) {
                if (!expected.getMessage().contains("hết lượt")) throw expected;
            }
            invoiceDao.cancel(invoice.getId(), admin.getId());
            DiscountCode released = new JdbcDiscountCodeDao()
                    .findById(discountCode.getId()).orElseThrow();
            if (released.getUsedCount() != 0) {
                throw new IllegalStateException(
                        "Cancelling invoice did not release code usage.");
            }

            System.out.printf(
                    "businessWorkflowSmoke=true, order=%s, invoice=%s, stockRestored=%d%n",
                    order.getCode(), invoice.getCode(), originalStock
            );
        } finally {
            cleanup(invoice == null ? null : invoice.getId(),
                    order == null ? null : order.getId(),
                    discountCode == null ? null : discountCode.getId());
        }
    }

    private static void cleanup(Long invoiceId, Long orderId, Long discountCodeId)
            throws Exception {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (invoiceId != null) {
                    execute(connection,
                            "DELETE FROM stock_transactions WHERE reference_type='INVOICE' AND reference_id=?",
                            invoiceId);
                    execute(connection, "DELETE FROM invoices WHERE id=?", invoiceId);
                }
                if (orderId != null) {
                    execute(connection,
                            "DELETE FROM stock_transactions WHERE reference_type='PURCHASE_ORDER' AND reference_id=?",
                            orderId);
                    execute(connection, "DELETE FROM purchase_orders WHERE id=?", orderId);
                }
                if (discountCodeId != null) {
                    execute(connection, "DELETE FROM discount_codes WHERE id=?", discountCodeId);
                }
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private static void execute(Connection connection, String sql, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }
}
