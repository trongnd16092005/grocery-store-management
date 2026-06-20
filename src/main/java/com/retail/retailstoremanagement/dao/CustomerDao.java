package com.retail.retailstoremanagement.dao;

import com.retail.retailstoremanagement.model.Customer;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface CustomerDao extends BaseDao<Customer> {
    List<Customer> search(String keyword, String customerType) throws SQLException;

    Optional<Customer> findByCode(String code) throws SQLException;

    boolean phoneExists(String phone, Long excludedId) throws SQLException;
}
