package com.retail.retailstoremanagement.dao;

import com.retail.retailstoremanagement.model.Supplier;
import java.sql.SQLException;
import java.util.List;

public interface SupplierDao extends BaseDao<Supplier> {
    List<Supplier> search(String keyword, boolean includeInactive) throws SQLException;
    boolean setActive(long id, boolean active) throws SQLException;
    boolean nameExists(String name, Long excludedId) throws SQLException;
}
