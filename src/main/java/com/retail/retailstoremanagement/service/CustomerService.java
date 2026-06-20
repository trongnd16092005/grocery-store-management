package com.retail.retailstoremanagement.service;

import com.retail.retailstoremanagement.dao.CustomerDao;
import com.retail.retailstoremanagement.dao.impl.JdbcCustomerDao;
import com.retail.retailstoremanagement.model.Customer;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class CustomerService {
    private final CustomerDao customerDao;

    public CustomerService() {
        this(new JdbcCustomerDao());
    }

    public CustomerService(CustomerDao customerDao) {
        this.customerDao = customerDao;
    }

    public List<Customer> search(String keyword, String customerType) throws SQLException {
        return customerDao.search(keyword, customerType);
    }

    public Customer findById(long id) throws SQLException {
        return customerDao.findById(id)
                .orElseThrow(() -> new ValidationException("Không tìm thấy khách hàng."));
    }

    public Optional<Customer> findByCode(String code) throws SQLException {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return customerDao.findByCode(code.trim().toUpperCase());
    }

    public Customer save(Customer customer) throws SQLException {
        normalize(customer);
        validate(customer);
        if (customerDao.phoneExists(customer.getPhone(), customer.getId())) {
            throw new ValidationException("Số điện thoại đã thuộc về khách hàng khác.");
        }
        if (customer.getId() == null) {
            return customerDao.insert(customer);
        }
        Customer existing = findById(customer.getId());
        customer.setActive(existing.isActive());
        if (!customerDao.update(customer)) {
            throw new ValidationException("Không thể cập nhật khách hàng.");
        }
        return customer;
    }

    private void normalize(Customer customer) {
        if (customer.getPhone() != null) {
            customer.setPhone(customer.getPhone().replaceAll("[^0-9]", ""));
        }
        if (customer.getEmail() != null) {
            customer.setEmail(customer.getEmail().trim().toLowerCase());
        }
    }

    private void validate(Customer customer) {
        if (customer.getFullName() == null || customer.getFullName().isBlank()) {
            throw new ValidationException("Họ tên khách hàng không được để trống.");
        }
        if (customer.getPhone() == null || !customer.getPhone().matches("[0-9]{9,11}")) {
            throw new ValidationException("Số điện thoại phải có từ 9 đến 11 chữ số.");
        }
        if (customer.getEmail() != null && !customer.getEmail().isBlank()
                && !customer.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new ValidationException("Email không đúng định dạng.");
        }
    }
}
