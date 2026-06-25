package com.retail.retailstoremanagement.dao;

import com.retail.retailstoremanagement.model.AppUser;
import com.retail.retailstoremanagement.model.Store;

import java.sql.SQLException;

public interface StoreDao {
    Long findActiveIdByCode(String code) throws SQLException;
    AppUser register(String storeCode, String storeName, String phone, String address,
                     String username, String passwordHash, String fullName) throws SQLException;
    Store findCurrent() throws SQLException;
    Store updateCurrent(String name, String phone, String address) throws SQLException;
    Store updateCurrentPayOs(boolean enabled, String clientId, String apiKey,
                             String checksumKey) throws SQLException;
    Store findPayOsForStoreId(long storeId) throws SQLException;
}
