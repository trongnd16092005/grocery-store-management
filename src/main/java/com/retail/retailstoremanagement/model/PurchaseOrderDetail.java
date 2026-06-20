package com.retail.retailstoremanagement.model;

import java.math.BigDecimal;

public class PurchaseOrderDetail extends BaseEntity {
    private Long purchaseOrderId;
    private Long productId;
    private int quantity;
    private BigDecimal unitCost = BigDecimal.ZERO;
    private BigDecimal lineTotal = BigDecimal.ZERO;

    public Long getPurchaseOrderId() {
        return purchaseOrderId;
    }

    public void setPurchaseOrderId(Long purchaseOrderId) {
        this.purchaseOrderId = purchaseOrderId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }

    public BigDecimal calculateLineTotal() {
        return unitCost.multiply(BigDecimal.valueOf(quantity));
    }
}
