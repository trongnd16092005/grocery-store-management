package com.retail.retailstoremanagement.service;

import com.retail.retailstoremanagement.dao.impl.*;
import com.retail.retailstoremanagement.model.*;
import com.retail.retailstoremanagement.util.DatabaseConnection;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Map;

/** Manual integration test; all temporary records are removed. */
public final class SalesAuthSmokeTest {
    public static void main(String[] args)throws Exception{
        Category category=new Category(); Product product=new Product(); Invoice invoice=null; Long tempUserId=null;
        CategoryService categories=new CategoryService(); ProductService products=new ProductService();
        try{
            category.setName("__SALES_SMOKE__"); categories.save(category);
            product.setName("__SALES_SMOKE_PRODUCT__");product.setBarcode("SALE-SMOKE-"+System.currentTimeMillis());product.setCategoryId(category.getId());product.setSellingPrice(BigDecimal.valueOf(2500));product.setMinimumStock(1);product.setUnit("cái");products.save(product);
            setStock(product.getId(),5);
            InvoiceService invoices=new InvoiceService(new JdbcInvoiceDao());
            invoice=invoices.checkout(Map.of(product.getCode(),2),null,"CASH",BigDecimal.valueOf(10000),null);
            if(products.findById(product.getId()).getStockQuantity()!=3)throw new IllegalStateException("Checkout did not decrement stock.");
            invoices.cancel(invoice.getId(),null);
            if(products.findById(product.getId()).getStockQuantity()!=5)throw new IllegalStateException("Cancellation did not restore stock.");
            try{invoices.cancel(invoice.getId(),null);throw new IllegalStateException("Invoice was cancelled twice.");}catch(SQLException expected){/* correct */}
            JdbcUserDao userDao=new JdbcUserDao();
            if(userDao.count()==0){AuthService auth=new AuthService(userDao);AppUser u=auth.setupAdmin("smoke_admin","Temporary123!","Smoke Admin");tempUserId=u.getId();if(auth.login("smoke_admin","Temporary123!")==null)throw new IllegalStateException("BCrypt login failed.");}
            System.out.printf("salesAuthSmoke=true, invoice=%s, restoredStock=5%n",invoice.getCode());
        }finally{cleanup(invoice==null?null:invoice.getId(),product.getId(),category.getId(),tempUserId);}
    }
    private static void cleanup(Long invoiceId,Long productId,Long categoryId,Long userId)throws Exception{try(Connection c=DatabaseConnection.getConnection()){c.setAutoCommit(false);try{if(productId!=null)exec(c,"DELETE FROM stock_transactions WHERE product_id=?",productId);if(invoiceId!=null)exec(c,"DELETE FROM invoices WHERE id=?",invoiceId);if(productId!=null)exec(c,"DELETE FROM products WHERE id=?",productId);if(categoryId!=null)exec(c,"DELETE FROM categories WHERE id=?",categoryId);if(userId!=null)exec(c,"DELETE FROM app_users WHERE id=?",userId);c.commit();}catch(Exception e){c.rollback();throw e;}}}
    private static void exec(Connection c,String sql,long id)throws SQLException{try(PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,id);s.executeUpdate();}}
    private static void setStock(long id,int stock)throws SQLException{try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement("UPDATE products SET stock_quantity=? WHERE id=?")){s.setInt(1,stock);s.setLong(2,id);s.executeUpdate();}}
}
