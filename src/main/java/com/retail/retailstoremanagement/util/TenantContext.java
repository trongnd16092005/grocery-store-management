package com.retail.retailstoremanagement.util;

public final class TenantContext {
    private static final ThreadLocal<Long> STORE_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setStoreId(Long storeId) {
        STORE_ID.set(storeId);
    }

    public static Long getStoreId() {
        return STORE_ID.get();
    }

    public static long requireStoreId() {
        Long storeId = STORE_ID.get();
        if (storeId == null) {
            throw new IllegalStateException("Store context is missing.");
        }
        return storeId;
    }

    public static void clear() {
        STORE_ID.remove();
    }
}
