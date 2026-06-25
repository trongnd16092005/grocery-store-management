package com.retail.retailstoremanagement.dao;

import com.retail.retailstoremanagement.model.Store;

import java.sql.SQLException;
import java.util.List;

public interface SuperAdminDao {
    List<Store> findStores() throws SQLException;
    boolean setStoreActive(long storeId, boolean active) throws SQLException;
}
