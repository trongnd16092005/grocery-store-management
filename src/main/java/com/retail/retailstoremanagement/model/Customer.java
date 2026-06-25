package com.retail.retailstoremanagement.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class Customer extends AuditableEntity {
    private String code;
    private String fullName;
    private String phone;
    private String email;
    private Gender gender;
    private String address;
    private CustomerType customerType = CustomerType.REGULAR;
    private boolean active = true;
    private int loyaltyPoints;
    private int lifetimeLoyaltyPoints;
    private long purchaseCount;
    private BigDecimal totalSpent = BigDecimal.ZERO;
    private OffsetDateTime lastPurchaseAt;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public CustomerType getCustomerType() {
        return customerType;
    }

    public void setCustomerType(CustomerType customerType) {
        this.customerType = customerType;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getLoyaltyPoints() {
        return loyaltyPoints;
    }

    public void setLoyaltyPoints(int loyaltyPoints) {
        this.loyaltyPoints = loyaltyPoints;
    }

    public int getLifetimeLoyaltyPoints() {
        return lifetimeLoyaltyPoints;
    }

    public void setLifetimeLoyaltyPoints(int lifetimeLoyaltyPoints) {
        this.lifetimeLoyaltyPoints = lifetimeLoyaltyPoints;
    }

    public long getPurchaseCount() {
        return purchaseCount;
    }

    public void setPurchaseCount(long purchaseCount) {
        this.purchaseCount = purchaseCount;
    }

    public BigDecimal getTotalSpent() {
        return totalSpent;
    }

    public void setTotalSpent(BigDecimal totalSpent) {
        this.totalSpent = totalSpent;
    }

    public OffsetDateTime getLastPurchaseAt() {
        return lastPurchaseAt;
    }

    public void setLastPurchaseAt(OffsetDateTime lastPurchaseAt) {
        this.lastPurchaseAt = lastPurchaseAt;
    }
}
