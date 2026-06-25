package com.retail.retailstoremanagement.service;

import com.retail.retailstoremanagement.dao.SuperAdminDao;
import com.retail.retailstoremanagement.dao.impl.JdbcSuperAdminDao;
import com.retail.retailstoremanagement.model.Store;

import java.sql.SQLException;
import java.util.List;

public class SuperAdminService {
    private final SuperAdminDao dao;

    public SuperAdminService() {
        this(new JdbcSuperAdminDao());
    }

    public SuperAdminService(SuperAdminDao dao) {
        this.dao = dao;
    }

    public List<Store> findStores() throws SQLException {
        return dao.findStores();
    }

    public void setStoreActive(long storeId, boolean active) throws SQLException {
        if (!dao.setStoreActive(storeId, active)) {
            throw new ValidationException("Không tìm thấy cửa hàng cần cập nhật.");
        }
    }
}
