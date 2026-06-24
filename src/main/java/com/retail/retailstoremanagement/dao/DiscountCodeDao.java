package com.retail.retailstoremanagement.dao;

import com.retail.retailstoremanagement.model.DiscountCode;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface DiscountCodeDao {
    List<DiscountCode> findAll(boolean includeInactive) throws SQLException;
    Optional<DiscountCode> findById(long id) throws SQLException;
    Optional<DiscountCode> findByCode(String code) throws SQLException;
    Optional<DiscountCode> lockByCode(Connection connection, String code) throws SQLException;
    DiscountCode insert(DiscountCode code) throws SQLException;
    boolean update(DiscountCode code) throws SQLException;
    boolean setActive(long id, boolean active) throws SQLException;
    void incrementUsage(Connection connection, long id) throws SQLException;
}
