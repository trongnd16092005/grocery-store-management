package com.retail.retailstoremanagement.model;

public class Store extends AuditableEntity {
    private String code;
    private String name;
    private String phone;
    private String address;
    private boolean active = true;
    private long adminCount;
    private long employeeCount;

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
}
