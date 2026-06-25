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

    public Store updatePayOs(boolean enabled, String clientId, String apiKey,
                             String checksumKey) throws SQLException {
        String normalizedClientId = normalize(clientId);
        String normalizedApiKey = normalize(apiKey);
        String normalizedChecksumKey = normalize(checksumKey);
        Store current = dao.findCurrent();
        boolean hasClientId = present(normalizedClientId) || present(current.getPayOsClientId());
        boolean hasApiKey = present(normalizedApiKey) || present(current.getPayOsApiKey());
        boolean hasChecksumKey = present(normalizedChecksumKey) || present(current.getPayOsChecksumKey());
        if (enabled && (!hasClientId || !hasApiKey || !hasChecksumKey)) {
            throw new ValidationException("Cần đủ Client ID, API Key và Checksum Key để bật QR payOS.");
        }
        if (present(normalizedClientId) && normalizedClientId.length() > 200) {
            throw new ValidationException("Client ID quá dài.");
        }
        if (present(normalizedApiKey) && normalizedApiKey.length() > 500) {
            throw new ValidationException("API Key quá dài.");
        }
        if (present(normalizedChecksumKey) && normalizedChecksumKey.length() > 500) {
            throw new ValidationException("Checksum Key quá dài.");
        }
        return dao.updateCurrentPayOs(enabled, normalizedClientId,
                normalizedApiKey, normalizedChecksumKey);
    }

    public Store findPayOsForStoreId(long storeId) throws SQLException {
        return dao.findPayOsForStoreId(storeId);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }
}
