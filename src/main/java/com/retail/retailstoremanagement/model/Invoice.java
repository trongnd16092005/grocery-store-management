package com.retail.retailstoremanagement.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class Invoice extends BaseEntity {
    private String code;
    private Long customerId;
    private Long cashierId;
    private String customerName;
    private String cashierName;
    private PaymentMethod paymentMethod;
    private InvoiceStatus status = InvoiceStatus.PAID;
    private BigDecimal subtotal = BigDecimal.ZERO;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal cashReceived;
    private BigDecimal changeAmount;
    private String note;
    private OffsetDateTime createdAt;
    private OffsetDateTime cancelledAt;
    private List<InvoiceDetail> details = new ArrayList<>();

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getCashierId() {
        return cashierId;
    }

    public void setCashierId(Long cashierId) {
        this.cashierId = cashierId;
    }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCashierName() { return cashierName; }
    public void setCashierName(String cashierName) { this.cashierName = cashierName; }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getCashReceived() {
        return cashReceived;
    }

    public void setCashReceived(BigDecimal cashReceived) {
        this.cashReceived = cashReceived;
    }

    public BigDecimal getChangeAmount() {
        return changeAmount;
    }

    public void setChangeAmount(BigDecimal changeAmount) {
        this.changeAmount = changeAmount;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(OffsetDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public List<InvoiceDetail> getDetails() {
        return details;
    }

    public void setDetails(List<InvoiceDetail> details) {
        this.details = details == null ? new ArrayList<>() : details;
    }

    public BigDecimal calculateTotalAmount() {
        return subtotal.subtract(discountAmount);
    }
}
