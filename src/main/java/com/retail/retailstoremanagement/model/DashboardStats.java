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
    private List<Invoice> recentInvoices = new ArrayList<>();

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
    public List<Invoice> getRecentInvoices() { return recentInvoices; }
    public void setRecentInvoices(List<Invoice> value) { recentInvoices = value; }
}
