package com.retail.retailstoremanagement.service;

import com.retail.retailstoremanagement.dao.SupplierDao;
import com.retail.retailstoremanagement.dao.impl.JdbcSupplierDao;
import com.retail.retailstoremanagement.model.Supplier;

import java.sql.SQLException;
import java.util.List;

public class SupplierService {
    private final SupplierDao dao;

    public SupplierService() {
        this(new JdbcSupplierDao());
    }

    public SupplierService(SupplierDao dao) {
        this.dao = dao;
    }

    public List<Supplier> search(String keyword, boolean includeInactive) throws SQLException {
        return dao.search(keyword, includeInactive);
    }

    public Supplier find(long id) throws SQLException {
        return dao.findById(id)
                .orElseThrow(() -> new ValidationException("Không tìm thấy nhà cung cấp."));
    }

    public Supplier save(Supplier supplier) throws SQLException {
        normalizeAndValidate(supplier);
        if (dao.nameExists(supplier.getName(), supplier.getId())) {
            throw new ValidationException("Tên nhà cung cấp đã tồn tại.");
        }
        if (supplier.getId() == null) return dao.insert(supplier);
        if (!dao.update(supplier)) {
            throw new ValidationException("Không tìm thấy nhà cung cấp cần cập nhật.");
        }
        return supplier;
    }

    public void setActive(long id, boolean active) throws SQLException {
        find(id);
        if (!dao.setActive(id, active)) {
            throw new ValidationException("Không thể cập nhật trạng thái nhà cung cấp.");
        }
    }

    private void normalizeAndValidate(Supplier supplier) {
        supplier.setName(trim(supplier.getName()));
        supplier.setPhone(trim(supplier.getPhone()).replaceAll("[^0-9]", ""));
        supplier.setEmail(trim(supplier.getEmail()).toLowerCase());
        supplier.setAddress(trim(supplier.getAddress()));

        if (supplier.getName().length() < 2 || supplier.getName().length() > 150) {
            throw new ValidationException("Tên nhà cung cấp cần từ 2 đến 150 ký tự.");
        }
        if (!supplier.getPhone().isEmpty()
                && !supplier.getPhone().matches("[0-9]{9,11}")) {
            throw new ValidationException("Số điện thoại cần từ 9 đến 11 chữ số.");
        }
        if (!supplier.getEmail().isEmpty()
                && !supplier.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new ValidationException("Email không đúng định dạng.");
        }
        if (supplier.getEmail().length() > 150) {
            throw new ValidationException("Email không được vượt quá 150 ký tự.");
        }
        if (supplier.getAddress().length() > 500) {
            throw new ValidationException("Địa chỉ không được vượt quá 500 ký tự.");
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
