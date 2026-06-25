package com.retail.retailstoremanagement.model;

public class Store extends AuditableEntity {
    private String code;
    private String name;
    private String phone;
    private String address;
    private boolean active = true;
    private long adminCount;
    private long employeeCount;
    private boolean payOsEnabled;
    private String payOsClientId;
    private String payOsApiKey;
    private String payOsChecksumKey;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public long getAdminCount() { return adminCount; }
    public void setAdminCount(long adminCount) { this.adminCount = adminCount; }
    public long getEmployeeCount() { return employeeCount; }
    public void setEmployeeCount(long employeeCount) { this.employeeCount = employeeCount; }
    public boolean isPayOsEnabled() { return payOsEnabled; }
    public void setPayOsEnabled(boolean payOsEnabled) { this.payOsEnabled = payOsEnabled; }
    public String getPayOsClientId() { return payOsClientId; }
    public void setPayOsClientId(String payOsClientId) { this.payOsClientId = payOsClientId; }
    public String getPayOsApiKey() { return payOsApiKey; }
    public void setPayOsApiKey(String payOsApiKey) { this.payOsApiKey = payOsApiKey; }
    public String getPayOsChecksumKey() { return payOsChecksumKey; }
    public void setPayOsChecksumKey(String payOsChecksumKey) { this.payOsChecksumKey = payOsChecksumKey; }
    public boolean hasPayOsCredentials() {
        return present(payOsClientId) && present(payOsApiKey) && present(payOsChecksumKey);
    }
    public boolean isPayOsConfigured() {
        return payOsEnabled && hasPayOsCredentials();
    }
    public String getPayOsClientIdMask() { return mask(payOsClientId); }
    public String getPayOsApiKeyMask() { return mask(payOsApiKey); }
    public String getPayOsChecksumKeyMask() { return mask(payOsChecksumKey); }

    private boolean present(String value) {
        return value != null && !value.isBlank();
    }

    private String mask(String value) {
        if (!present(value)) return "";
        String trimmed = value.trim();
        if (trimmed.length() <= 8) return "••••" + trimmed.substring(Math.max(0, trimmed.length() - 2));
        return trimmed.substring(0, 4) + "••••••" + trimmed.substring(trimmed.length() - 4);
    }
}
