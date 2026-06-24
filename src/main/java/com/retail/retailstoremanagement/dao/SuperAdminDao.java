package com.retail.retailstoremanagement.dao;

import com.retail.retailstoremanagement.model.AppUser;
import com.retail.retailstoremanagement.model.Store;

import java.sql.SQLException;
import java.util.List;

public interface SuperAdminDao {
    int countSuperAdmins() throws SQLException;
    AppUser createFirstSuperAdmin(String username, String passwordHash, String fullName)
            throws SQLException;
    List<Store> findStores() throws SQLException;
    boolean setStoreActive(long storeId, boolean active) throws SQLException;
}
