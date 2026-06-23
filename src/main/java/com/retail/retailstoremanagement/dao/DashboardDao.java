package com.retail.retailstoremanagement.dao;
import com.retail.retailstoremanagement.model.DashboardStats;
import java.sql.SQLException;
public interface DashboardDao { DashboardStats load() throws SQLException; }
