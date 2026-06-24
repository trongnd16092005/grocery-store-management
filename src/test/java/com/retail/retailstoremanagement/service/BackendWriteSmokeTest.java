package com.retail.retailstoremanagement.service;

import com.retail.retailstoremanagement.dao.impl.JdbcSupplierDao;
import com.retail.retailstoremanagement.model.Category;
import com.retail.retailstoremanagement.model.Customer;
import com.retail.retailstoremanagement.model.CustomerType;
import com.retail.retailstoremanagement.model.Product;
import com.retail.retailstoremanagement.model.Supplier;
import com.retail.retailstoremanagement.util.DatabaseConnection;
import com.retail.retailstoremanagement.util.TestTenantContext;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Manual write integration test. It removes every record it creates.
 */
public final class BackendWriteSmokeTest {
    private BackendWriteSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        TestTenantContext.activateDefaultStore();
        CategoryService categoryService = new CategoryService();
        ProductService productService = new ProductService();
        CustomerService customerService = new CustomerService();
        InventoryService inventoryService = new InventoryService();
        Supplier supplier = new JdbcSupplierDao().findAll().get(0);

        Category category = new Category();
        Product product = new Product();
        Customer customer = new Customer();

        try {
            category.setName("__SMOKE_CATEGORY__");
            category.setDescription("Temporary integration test");
            categoryService.save(category);

            product.setName("__SMOKE_PRODUCT__");
            product.setBarcode("SMOKE-" + System.currentTimeMillis());
            product.setCategoryId(category.getId());
            product.setSupplierId(supplier.getId());
            product.setCostPrice(BigDecimal.valueOf(1000));
            product.setSellingPrice(BigDecimal.valueOf(1500));
            product.setMinimumStock(2);
            product.setUnit("cái");
            productService.save(product);

            customer.setFullName("Smoke Test Customer");
            customer.setPhone("0999999001");
            customer.setCustomerType(CustomerType.REGULAR);
            customerService.save(customer);

            inventoryService.importStock(
                    product.getId(), 5, BigDecimal.valueOf(1100),
                    supplier.getId(), "Smoke import", null
            );
            if (productService.findById(product.getId()).getStockQuantity() != 5) {
                throw new IllegalStateException("Import stock result is incorrect.");
            }

            inventoryService.adjustStock(product.getId(), 3, "Smoke adjustment", null);
            if (productService.findById(product.getId()).getStockQuantity() != 3) {
                throw new IllegalStateException("Adjust stock result is incorrect.");
            }

            System.out.printf(
                    "writeSmoke=true, category=%s, product=%s, customer=%s, finalStock=3%n",
                    category.getCode(), product.getCode(), customer.getCode()
            );
        } finally {
            cleanup(product.getId(), customer.getId(), category.getId());
        }
    }

    private static void cleanup(Long productId, Long customerId, Long categoryId) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (productId != null) {
                    execute(connection, "DELETE FROM stock_transactions WHERE product_id = ?", productId);
                    execute(connection,
                            "DELETE FROM purchase_orders WHERE id IN "
                                    + "(SELECT purchase_order_id FROM purchase_order_details WHERE product_id = ?)",
                            productId);
                    execute(connection, "DELETE FROM products WHERE id = ?", productId);
                }
                if (customerId != null) {
                    execute(connection, "DELETE FROM customers WHERE id = ?", customerId);
                }
                if (categoryId != null) {
                    execute(connection, "DELETE FROM categories WHERE id = ?", categoryId);
                }
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private static void execute(Connection connection, String sql, long id) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }
}
