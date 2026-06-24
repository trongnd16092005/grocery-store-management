package com.retail.retailstoremanagement.service;

import com.retail.retailstoremanagement.dao.StoreDao;
import com.retail.retailstoremanagement.dao.impl.JdbcStoreDao;
import com.retail.retailstoremanagement.model.Store;

import java.sql.SQLException;

public class StoreService {
    private final StoreDao dao;

    public StoreService() {
        this(new JdbcStoreDao());
    }

    public StoreService(StoreDao dao) {
        this.dao = dao;
    }

    public Store findCurrent() throws SQLException {
        return dao.findCurrent();
    }

    public Store update(String name, String phone, String address) throws SQLException {
        if (name == null || name.trim().length() < 2 || name.trim().length() > 150) {
            throw new ValidationException("Tên cửa hàng cần từ 2 đến 150 ký tự.");
        }
        String normalizedPhone = phone == null ? "" : phone.replaceAll("[^0-9]", "");
        if (!normalizedPhone.isEmpty() && !normalizedPhone.matches("[0-9]{9,11}")) {
            throw new ValidationException("Số điện thoại cần từ 9 đến 11 chữ số.");
        }
        if (address != null && address.length() > 500) {
            throw new ValidationException("Địa chỉ không được vượt quá 500 ký tự.");
        }
        return dao.updateCurrent(name.trim(), normalizedPhone, address == null ? "" : address.trim());
    }
}
