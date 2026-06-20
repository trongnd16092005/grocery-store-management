package com.retail.retailstoremanagement.service;

/**
 * Read-only integration smoke test for modules 4-6.
 * Run with DB_PASSWORD set in the environment.
 */
public final class BackendModuleSmokeTest {
    private BackendModuleSmokeTest() {
    }

    public static void main(String[] args) throws Exception {
        CategoryService categoryService = new CategoryService();
        ProductService productService = new ProductService();
        CustomerService customerService = new CustomerService();
        InventoryService inventoryService = new InventoryService();

        int categoryCount = categoryService.findAll().size();
        long productCount = productService.count("", null, "");
        long lowStockCount = productService.count("", null, "LOW");
        long outOfStockCount = productService.count("", null, "OUT");
        boolean customerFound = customerService.findByCode("KH001").isPresent();
        int historyCount = inventoryService.findRecentTransactions(30).size();

        if (categoryCount != 4 || productCount != 8 || !customerFound) {
            throw new IllegalStateException("Unexpected demo data returned by backend modules.");
        }

        System.out.printf(
                "categories=%d, products=%d, lowStock=%d, outOfStock=%d, "
                        + "customerKH001=%s, stockHistory=%d%n",
                categoryCount, productCount, lowStockCount, outOfStockCount,
                customerFound, historyCount
        );
    }
}
