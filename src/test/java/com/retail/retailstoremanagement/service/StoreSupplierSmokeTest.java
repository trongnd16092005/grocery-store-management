package com.retail.retailstoremanagement.service;

import com.retail.retailstoremanagement.model.Store;
import com.retail.retailstoremanagement.model.Supplier;
import com.retail.retailstoremanagement.util.DatabaseConnection;
import com.retail.retailstoremanagement.util.TestTenantContext;

import java.sql.Connection;
import java.sql.PreparedStatement;

/**
 * Manual integration test for store profile and supplier management.
 * The temporary supplier is removed before the test exits.
 */
public final class StoreSupplierSmokeTest {
    private StoreSupplierSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        TestTenantContext.activateDefaultStore();
        StoreService storeService = new StoreService();
        SupplierService supplierService = new SupplierService();

        Store store = storeService.findCurrent();
        if (store == null || store.getId() == null || store.getCode() == null) {
            throw new IllegalStateException("Current store could not be loaded.");
        }

        Supplier supplier = new Supplier();
        try {
            supplier.setName("__SMOKE_SUPPLIER_" + System.currentTimeMillis() + "__");
            supplier.setPhone("0901234567");
            supplier.setEmail("supplier-smoke@example.com");
            supplier.setAddress("Temporary integration test");
            supplierService.save(supplier);

            supplier.setName(supplier.getName() + "_UPDATED");
            supplierService.save(supplier);
            supplierService.setActive(supplier.getId(), false);

            Supplier saved = supplierService.find(supplier.getId());
            if (saved.isActive() || !saved.getName().endsWith("_UPDATED")) {
                throw new IllegalStateException("Supplier update/status result is incorrect.");
            }
            supplierService.setActive(supplier.getId(), true);

            System.out.printf(
                    "storeSupplierSmoke=true, store=%s, supplier=%s%n",
                    store.getCode(), supplier.getCode()
            );
        } finally {
            if (supplier.getId() != null) deleteSupplier(supplier.getId());
        }
    }

    private static void deleteSupplier(long id) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement("DELETE FROM suppliers WHERE id=?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }
}
