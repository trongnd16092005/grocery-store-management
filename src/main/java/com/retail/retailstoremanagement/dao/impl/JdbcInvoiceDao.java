package com.retail.retailstoremanagement.dao.impl;

import com.retail.retailstoremanagement.dao.InvoiceDao;
import com.retail.retailstoremanagement.model.*;
import com.retail.retailstoremanagement.util.DatabaseConnection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;

public class JdbcInvoiceDao implements InvoiceDao {
    @Override
    public Invoice checkout(Map<String, Integer> items, String customerCode, PaymentMethod method,
                            BigDecimal cashReceived, DiscountType discountType, BigDecimal discountValue,
                            Long cashierId) throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                Long customerId = findCustomer(c, customerCode);
                List<InvoiceDetail> details = new ArrayList<>();
                BigDecimal subtotal = BigDecimal.ZERO;
                for (Map.Entry<String,Integer> item : items.entrySet()) {
                    InvoiceDetail detail = lockProduct(c, item.getKey(), item.getValue());
                    details.add(detail);
                    subtotal = subtotal.add(detail.calculateLineTotal());
                }
                BigDecimal discountAmount = calculateDiscount(subtotal, discountType, discountValue);
                BigDecimal total = subtotal.subtract(discountAmount);
                BigDecimal change = null;
                if (method == PaymentMethod.CASH) {
                    if (cashReceived == null || cashReceived.compareTo(total) < 0)
                        throw new SQLException("Tiền khách đưa chưa đủ.");
                    change = cashReceived.subtract(total);
                }
                long invoiceId = insertInvoice(c, customerId, cashierId, method, subtotal, discountAmount, total,
                        method == PaymentMethod.CASH ? cashReceived : null, change);
                for (InvoiceDetail d : details) {
                    insertDetail(c, invoiceId, d);
                    decreaseStock(c, d.getProductId(), d.getQuantity(), invoiceId, cashierId);
                }
                c.commit();
                return findById(invoiceId);
            } catch (Exception e) {
                c.rollback();
                if (e instanceof SQLException) throw (SQLException)e;
                throw new SQLException("Không thể tạo hóa đơn.", e);
            } finally { c.setAutoCommit(true); }
        }
    }

    /** Tính số tiền giảm giá dựa trên subtotal đã chốt ở server, không tin số FE gửi lên. */
    private BigDecimal calculateDiscount(BigDecimal subtotal, DiscountType type, BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        BigDecimal amount = type == DiscountType.PERCENT
                ? subtotal.multiply(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(subtotal) > 0) amount = subtotal;
        if (amount.compareTo(BigDecimal.ZERO) < 0) amount = BigDecimal.ZERO;
        return amount;
    }

    private Long findCustomer(Connection c, String code) throws SQLException {
        if (code == null) return null;
        try (PreparedStatement s = c.prepareStatement("SELECT id FROM customers WHERE UPPER(code)=? AND active")) {
            s.setString(1, code.toUpperCase());
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) throw new SQLException("Không tìm thấy khách hàng " + code + ".");
                return r.getLong(1);
            }
        }
    }

    private InvoiceDetail lockProduct(Connection c, String code, int quantity) throws SQLException {
        String sql = "SELECT id, code, name, selling_price, stock_quantity FROM products "
                + "WHERE UPPER(code)=? AND active FOR UPDATE";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, code.toUpperCase());
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) throw new SQLException("Sản phẩm " + code + " không tồn tại hoặc đã ngừng bán.");
                int stock = r.getInt("stock_quantity");
                if (stock < quantity) throw new SQLException("Sản phẩm " + r.getString("name")
                        + " chỉ còn " + stock + " trong kho.");
                InvoiceDetail d = new InvoiceDetail();
                d.setProductId(r.getLong("id")); d.setProductCode(r.getString("code"));
                d.setProductName(r.getString("name")); d.setUnitPrice(r.getBigDecimal("selling_price"));
                d.setQuantity(quantity); d.setLineTotal(d.calculateLineTotal());
                return d;
            }
        }
    }

    private long insertInvoice(Connection c, Long customerId, Long cashierId, PaymentMethod method,
                               BigDecimal subtotal, BigDecimal discountAmount, BigDecimal total,
                               BigDecimal cash, BigDecimal change) throws SQLException {
        String sql = "INSERT INTO invoices(code,customer_id,cashier_id,payment_method,status,subtotal,discount_amount,total_amount,cash_received,change_amount) "
                + "VALUES ('HD'||LPAD(nextval('invoice_code_seq')::text,3,'0'),?,?,?,'PAID',?,?,?,?,?) RETURNING id";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            nullableLong(s,1,customerId); nullableLong(s,2,cashierId); s.setString(3,method.name());
            s.setBigDecimal(4,subtotal); s.setBigDecimal(5,discountAmount); s.setBigDecimal(6,total);
            nullableDecimal(s,7,cash); nullableDecimal(s,8,change);
            try(ResultSet r=s.executeQuery()){ r.next(); return r.getLong(1); }
        }
    }

    private void insertDetail(Connection c, long invoiceId, InvoiceDetail d) throws SQLException {
        try(PreparedStatement s=c.prepareStatement("INSERT INTO invoice_details(invoice_id,product_id,product_code,product_name,unit_price,quantity) VALUES (?,?,?,?,?,?)")){
            s.setLong(1,invoiceId); s.setLong(2,d.getProductId()); s.setString(3,d.getProductCode());
            s.setString(4,d.getProductName()); s.setBigDecimal(5,d.getUnitPrice()); s.setInt(6,d.getQuantity()); s.executeUpdate();
        }
    }

    private void decreaseStock(Connection c,long productId,int quantity,long invoiceId,Long userId)throws SQLException{
        int before;
        try(PreparedStatement s=c.prepareStatement("SELECT stock_quantity FROM products WHERE id=? FOR UPDATE")){s.setLong(1,productId);try(ResultSet r=s.executeQuery()){r.next();before=r.getInt(1);}}
        int after=before-quantity;
        if(after<0) throw new SQLException("Tồn kho vừa thay đổi, vui lòng thử lại.");
        try(PreparedStatement s=c.prepareStatement("UPDATE products SET stock_quantity=? WHERE id=?")){s.setInt(1,after);s.setLong(2,productId);s.executeUpdate();}
        insertStockLog(c,productId,"SALE",-quantity,before,after,invoiceId,"Xuất bán",userId);
    }

    @Override public List<Invoice> findAll(String keyword,String status,int limit,int offset)throws SQLException{
        String sql="SELECT i.*,c.full_name customer_name,u.full_name cashier_name FROM invoices i LEFT JOIN customers c ON c.id=i.customer_id LEFT JOIN app_users u ON u.id=i.cashier_id WHERE (?='' OR LOWER(i.code) LIKE LOWER(?) OR LOWER(COALESCE(c.full_name,'')) LIKE LOWER(?)) AND (?='' OR i.status=?) ORDER BY i.created_at DESC LIMIT ? OFFSET ?";
        List<Invoice> list=new ArrayList<>(); String q=keyword==null?"":keyword.trim(); String st=status==null?"":status.trim();
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setString(1,q);s.setString(2,"%"+q+"%");s.setString(3,"%"+q+"%");s.setString(4,st);s.setString(5,st);s.setInt(6,limit);s.setInt(7,offset);try(ResultSet r=s.executeQuery()){while(r.next())list.add(map(r));}}
        return list;
    }

    @Override public Invoice findById(long id)throws SQLException{
        String sql="SELECT i.*,c.full_name customer_name,u.full_name cashier_name FROM invoices i LEFT JOIN customers c ON c.id=i.customer_id LEFT JOIN app_users u ON u.id=i.cashier_id WHERE i.id=?";
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,id);try(ResultSet r=s.executeQuery()){if(!r.next())return null;Invoice i=map(r);i.setDetails(findDetails(c,id));return i;}}
    }

    private List<InvoiceDetail> findDetails(Connection c,long id)throws SQLException{
        List<InvoiceDetail> list=new ArrayList<>();try(PreparedStatement s=c.prepareStatement("SELECT * FROM invoice_details WHERE invoice_id=? ORDER BY id")){s.setLong(1,id);try(ResultSet r=s.executeQuery()){while(r.next()){InvoiceDetail d=new InvoiceDetail();d.setId(r.getLong("id"));d.setInvoiceId(id);long pid=r.getLong("product_id");d.setProductId(r.wasNull()?null:pid);d.setProductCode(r.getString("product_code"));d.setProductName(r.getString("product_name"));d.setUnitPrice(r.getBigDecimal("unit_price"));d.setQuantity(r.getInt("quantity"));d.setLineTotal(r.getBigDecimal("line_total"));list.add(d);}}}return list;
    }

    @Override public void cancel(long id,Long userId)throws SQLException{
        try(Connection c=DatabaseConnection.getConnection()){c.setAutoCommit(false);try{
            try(PreparedStatement s=c.prepareStatement("SELECT status FROM invoices WHERE id=? FOR UPDATE")){s.setLong(1,id);try(ResultSet r=s.executeQuery()){if(!r.next())throw new SQLException("Hóa đơn không tồn tại.");if(!"PAID".equals(r.getString(1)))throw new SQLException("Hóa đơn đã hủy hoặc chưa thanh toán.");}}
            for(InvoiceDetail d:findDetails(c,id)){if(d.getProductId()==null)continue;int before;try(PreparedStatement s=c.prepareStatement("SELECT stock_quantity FROM products WHERE id=? FOR UPDATE")){s.setLong(1,d.getProductId());try(ResultSet r=s.executeQuery()){if(!r.next())continue;before=r.getInt(1);}}int after=before+d.getQuantity();try(PreparedStatement s=c.prepareStatement("UPDATE products SET stock_quantity=? WHERE id=?")){s.setInt(1,after);s.setLong(2,d.getProductId());s.executeUpdate();}insertStockLog(c,d.getProductId(),"CANCEL_SALE",d.getQuantity(),before,after,id,"Hủy hóa đơn",userId);}
            try(PreparedStatement s=c.prepareStatement("UPDATE invoices SET status='CANCELLED',cancelled_at=CURRENT_TIMESTAMP WHERE id=?")){s.setLong(1,id);s.executeUpdate();}c.commit();
        }catch(Exception e){c.rollback();if(e instanceof SQLException)throw(SQLException)e;throw new SQLException(e);}finally{c.setAutoCommit(true);}}
    }

    private void insertStockLog(Connection c,long productId,String type,int change,int before,int after,long invoiceId,String reason,Long userId)throws SQLException{
        try(PreparedStatement s=c.prepareStatement("INSERT INTO stock_transactions(product_id,transaction_type,quantity_change,stock_before,stock_after,reference_type,reference_id,reason,created_by) VALUES (?,?,?,?,?,'INVOICE',?,?,?)")){s.setLong(1,productId);s.setString(2,type);s.setInt(3,change);s.setInt(4,before);s.setInt(5,after);s.setLong(6,invoiceId);s.setString(7,reason);nullableLong(s,8,userId);s.executeUpdate();}
    }

    private Invoice map(ResultSet r)throws SQLException{Invoice i=new Invoice();i.setId(r.getLong("id"));i.setCode(r.getString("code"));long v=r.getLong("customer_id");i.setCustomerId(r.wasNull()?null:v);v=r.getLong("cashier_id");i.setCashierId(r.wasNull()?null:v);i.setCustomerName(r.getString("customer_name"));i.setCashierName(r.getString("cashier_name"));i.setPaymentMethod(PaymentMethod.valueOf(r.getString("payment_method")));i.setStatus(InvoiceStatus.valueOf(r.getString("status")));i.setSubtotal(r.getBigDecimal("subtotal"));i.setDiscountAmount(r.getBigDecimal("discount_amount"));i.setTotalAmount(r.getBigDecimal("total_amount"));i.setCashReceived(r.getBigDecimal("cash_received"));i.setChangeAmount(r.getBigDecimal("change_amount"));i.setNote(r.getString("note"));i.setCreatedAt(r.getObject("created_at",OffsetDateTime.class));i.setCancelledAt(r.getObject("cancelled_at",OffsetDateTime.class));return i;}
    private void nullableLong(PreparedStatement s,int i,Long v)throws SQLException{if(v==null)s.setNull(i,Types.BIGINT);else s.setLong(i,v);}
    private void nullableDecimal(PreparedStatement s,int i,BigDecimal v)throws SQLException{if(v==null)s.setNull(i,Types.NUMERIC);else s.setBigDecimal(i,v);}
}
