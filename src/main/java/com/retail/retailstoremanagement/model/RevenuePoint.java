package com.retail.retailstoremanagement.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RevenuePoint {
    private LocalDate date;
    private String label;
    private BigDecimal revenue = BigDecimal.ZERO;
    private int chartPercent;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public int getChartPercent() {
        return chartPercent;
    }

    public void setChartPercent(int chartPercent) {
        this.chartPercent = chartPercent;
    }
}
