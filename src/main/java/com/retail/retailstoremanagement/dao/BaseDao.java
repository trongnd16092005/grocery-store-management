package com.retail.retailstoremanagement.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Common contract for JDBC DAO implementations.
 */
public interface BaseDao<T> {
    List<T> findAll() throws SQLException;

    Optional<T> findById(long id) throws SQLException;

    T insert(T entity) throws SQLException;

    boolean update(T entity) throws SQLException;
}
