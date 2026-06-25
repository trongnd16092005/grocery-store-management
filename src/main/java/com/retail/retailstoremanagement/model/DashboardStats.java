package com.retail.retailstoremanagement.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DashboardStats {
    private long productCount;
    private long customerCount;
    private long lowStockCount;
    private long todayInvoiceCount;
    private BigDecimal todayRevenue = BigDecimal.ZERO;
    private BigDecimal sevenDayRevenue = BigDecimal.ZERO;
    private long cashPaymentCount;
    private long qrPaymentCount;
    private int cashPaymentPercent;
    private int qrPaymentPercent;
    private List<RevenuePoint> sevenDayRevenuePoints = new ArrayList<>();
    private List<Invoice> recentInvoices = new ArrayList<>();
    private List<Product> lowStockProducts = new ArrayList<>();

    public long getProductCount() { return productCount; }
    public void setProductCount(long value) { productCount = value; }
    public long getCustomerCount() { return customerCount; }
    public void setCustomerCount(long value) { customerCount = value; }
    public long getLowStockCount() { return lowStockCount; }
    public void setLowStockCount(long value) { lowStockCount = value; }
    public long getTodayInvoiceCount() { return todayInvoiceCount; }
    public void setTodayInvoiceCount(long value) { todayInvoiceCount = value; }
    public BigDecimal getTodayRevenue() { return todayRevenue; }
    public void setTodayRevenue(BigDecimal value) { todayRevenue = value; }
    public BigDecimal getSevenDayRevenue() { return sevenDayRevenue; }
    public void setSevenDayRevenue(BigDecimal value) { sevenDayRevenue = value; }
    public long getCashPaymentCount() { return cashPaymentCount; }
    public void setCashPaymentCount(long value) { cashPaymentCount = value; }
    public long getQrPaymentCount() { return qrPaymentCount; }
    public void setQrPaymentCount(long value) { qrPaymentCount = value; }
    public int getCashPaymentPercent() { return cashPaymentPercent; }
    public void setCashPaymentPercent(int value) { cashPaymentPercent = value; }
    public int getQrPaymentPercent() { return qrPaymentPercent; }
    public void setQrPaymentPercent(int value) { qrPaymentPercent = value; }
    public List<RevenuePoint> getSevenDayRevenuePoints() { return sevenDayRevenuePoints; }
    public void setSevenDayRevenuePoints(List<RevenuePoint> value) {
        sevenDayRevenuePoints = value;
    }
    public List<Invoice> getRecentInvoices() { return recentInvoices; }
    public void setRecentInvoices(List<Invoice> value) { recentInvoices = value; }
    public List<Product> getLowStockProducts() { return lowStockProducts; }
    public void setLowStockProducts(List<Product> value) { lowStockProducts = value; }
}
