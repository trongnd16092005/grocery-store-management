package com.retail.retailstoremanagement.dao.impl;

import com.retail.retailstoremanagement.dao.InvoiceDao;
import com.retail.retailstoremanagement.dao.impl.JdbcDiscountCodeDao;
import com.retail.retailstoremanagement.model.*;
import com.retail.retailstoremanagement.service.DiscountCodeService;
import com.retail.retailstoremanagement.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.*;

public class JdbcInvoiceDao implements InvoiceDao {
    private final JdbcDiscountCodeDao discountCodeDao = new JdbcDiscountCodeDao();
    private final DiscountCodeService discountCodeService =
            new DiscountCodeService(discountCodeDao);

    @Override
    public Invoice checkout(Map<String, Integer> items, String customerCode, PaymentMethod method,
                            BigDecimal cashReceived, String discountCode, Long cashierId)
            throws SQLException {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                CustomerInfo customer = findCustomer(c, customerCode);
                List<InvoiceDetail> details = new ArrayList<>();
                BigDecimal subtotal = BigDecimal.ZERO;
                for (Map.Entry<String,Integer> item : items.entrySet()) {
                    InvoiceDetail detail = lockProduct(c, item.getKey(), item.getValue());
                    details.add(detail);
                    subtotal = subtotal.add(detail.calculateLineTotal());
                }
                DiscountCode appliedCode = null;
                BigDecimal discountAmount = BigDecimal.ZERO;
                if (discountCode != null && !discountCode.isBlank()) {
                    appliedCode = discountCodeDao.lockByCode(c, discountCode)
                            .orElseThrow(() -> new SQLException("Mã giảm giá không tồn tại."));
                    try {
                        discountAmount = discountCodeService.calculateValidDiscount(
                                appliedCode, subtotal, OffsetDateTime.now(),
                                customer == null ? null : customer.type,
                                eligibleSubtotal(details, appliedCode));
                    } catch (RuntimeException exception) {
                        throw new SQLException(exception.getMessage(), exception);
                    }
                }
                BigDecimal total = subtotal.subtract(discountAmount);
                BigDecimal change = null;
                if (method == PaymentMethod.CASH) {
                    if (cashReceived == null || cashReceived.compareTo(total) < 0)
                        throw new SQLException("Tiền khách đưa chưa đủ.");
                    change = cashReceived.subtract(total);
                }
                long invoiceId = insertInvoice(c, customer == null ? null : customer.id, cashierId, method, subtotal,
                        discountAmount, total, method == PaymentMethod.CASH ? cashReceived : null,
                        change, appliedCode);
                for (InvoiceDetail d : details) {
                    insertDetail(c, invoiceId, d);
                    decreaseStock(c, d.getProductId(), d.getQuantity(), invoiceId, cashierId);
                }
                if (appliedCode != null) discountCodeDao.incrementUsage(c, appliedCode.getId());
                if (customer != null) refreshCustomerLoyalty(c, customer.id);
                c.commit();
                return findById(invoiceId);
            } catch (Exception e) {
                c.rollback();
                if (e instanceof SQLException) throw (SQLException)e;
                throw new SQLException("Không thể tạo hóa đơn.", e);
            } finally { c.setAutoCommit(true); }
        }
    }

    private CustomerInfo findCustomer(Connection c, String code) throws SQLException {
        if (code == null) return null;
        try (PreparedStatement s = c.prepareStatement(
                "SELECT id,customer_type FROM customers WHERE UPPER(code)=? AND active")) {
            s.setString(1, code.toUpperCase());
            try (ResultSet r = s.executeQuery()) {
                if (!r.next()) throw new SQLException("Không tìm thấy khách hàng " + code + ".");
                return new CustomerInfo(r.getLong("id"),
                        CustomerType.valueOf(r.getString("customer_type")));
            }
        }
    }

    private BigDecimal eligibleSubtotal(List<InvoiceDetail> details, DiscountCode code) {
        if (code == null || code.getProductId() == null) return null;
        BigDecimal total = BigDecimal.ZERO;
        for (InvoiceDetail detail : details) {
            if (code.getProductId().equals(detail.getProductId())) {
                total = total.add(detail.calculateLineTotal());
            }
        }
        return total;
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
                               BigDecimal cash, BigDecimal change, DiscountCode discountCode)
            throws SQLException {
        String sql = "INSERT INTO invoices(code,customer_id,cashier_id,payment_method,status,"
                + "subtotal,discount_amount,total_amount,cash_received,change_amount,"
                + "discount_code_id,discount_code) "
                + "VALUES ('HD'||LPAD(nextval('invoice_code_seq')::text,3,'0'),?,?,?,'PAID',?,?,?,?,?,?,?) RETURNING id";
        try (PreparedStatement s = c.prepareStatement(sql)) {
            nullableLong(s,1,customerId); nullableLong(s,2,cashierId); s.setString(3,method.name());
            s.setBigDecimal(4,subtotal); s.setBigDecimal(5,discountAmount); s.setBigDecimal(6,total);
            nullableDecimal(s,7,cash); nullableDecimal(s,8,change);
            nullableLong(s,9,discountCode == null ? null : discountCode.getId());
            s.setString(10,discountCode == null ? null : discountCode.getCode());
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
        String sql="SELECT i.*,c.full_name customer_name,u.full_name cashier_name,a.full_name discount_approver_name FROM invoices i LEFT JOIN customers c ON c.id=i.customer_id LEFT JOIN app_users u ON u.id=i.cashier_id LEFT JOIN app_users a ON a.id=i.discount_approved_by WHERE (?='' OR LOWER(i.code) LIKE LOWER(?) OR LOWER(COALESCE(c.full_name,'')) LIKE LOWER(?)) AND (?='' OR i.status=?) ORDER BY i.created_at DESC LIMIT ? OFFSET ?";
        List<Invoice> list=new ArrayList<>(); String q=keyword==null?"":keyword.trim(); String st=status==null?"":status.trim();
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setString(1,q);s.setString(2,"%"+q+"%");s.setString(3,"%"+q+"%");s.setString(4,st);s.setString(5,st);s.setInt(6,limit);s.setInt(7,offset);try(ResultSet r=s.executeQuery()){while(r.next())list.add(map(r));}}
        return list;
    }

    @Override public Invoice findById(long id)throws SQLException{
        String sql="SELECT i.*,c.full_name customer_name,u.full_name cashier_name,a.full_name discount_approver_name FROM invoices i LEFT JOIN customers c ON c.id=i.customer_id LEFT JOIN app_users u ON u.id=i.cashier_id LEFT JOIN app_users a ON a.id=i.discount_approved_by WHERE i.id=?";
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,id);try(ResultSet r=s.executeQuery()){if(!r.next())return null;Invoice i=map(r);i.setDetails(findDetails(c,id));return i;}}
    }

    private List<InvoiceDetail> findDetails(Connection c,long id)throws SQLException{
        List<InvoiceDetail> list=new ArrayList<>();try(PreparedStatement s=c.prepareStatement("SELECT * FROM invoice_details WHERE invoice_id=? ORDER BY id")){s.setLong(1,id);try(ResultSet r=s.executeQuery()){while(r.next()){InvoiceDetail d=new InvoiceDetail();d.setId(r.getLong("id"));d.setInvoiceId(id);long pid=r.getLong("product_id");d.setProductId(r.wasNull()?null:pid);d.setProductCode(r.getString("product_code"));d.setProductName(r.getString("product_name"));d.setUnitPrice(r.getBigDecimal("unit_price"));d.setQuantity(r.getInt("quantity"));d.setLineTotal(r.getBigDecimal("line_total"));list.add(d);}}}return list;
    }

    @Override public void cancel(long id,Long userId)throws SQLException{
        try(Connection c=DatabaseConnection.getConnection()){c.setAutoCommit(false);try{
            Long discountCodeId=null; Long customerId=null;
            try(PreparedStatement s=c.prepareStatement("SELECT status,discount_code_id,customer_id FROM invoices WHERE id=? FOR UPDATE")){s.setLong(1,id);try(ResultSet r=s.executeQuery()){if(!r.next())throw new SQLException("Hóa đơn không tồn tại.");if(!"PAID".equals(r.getString("status")))throw new SQLException("Hóa đơn đã hủy hoặc chưa thanh toán.");long value=r.getLong("discount_code_id");discountCodeId=r.wasNull()?null:value;value=r.getLong("customer_id");customerId=r.wasNull()?null:value;}}
            for(InvoiceDetail d:findDetails(c,id)){if(d.getProductId()==null)continue;int before;try(PreparedStatement s=c.prepareStatement("SELECT stock_quantity FROM products WHERE id=? FOR UPDATE")){s.setLong(1,d.getProductId());try(ResultSet r=s.executeQuery()){if(!r.next())continue;before=r.getInt(1);}}int after=before+d.getQuantity();try(PreparedStatement s=c.prepareStatement("UPDATE products SET stock_quantity=? WHERE id=?")){s.setInt(1,after);s.setLong(2,d.getProductId());s.executeUpdate();}insertStockLog(c,d.getProductId(),"CANCEL_SALE",d.getQuantity(),before,after,id,"Hủy hóa đơn",userId);}
            if(discountCodeId!=null){try(PreparedStatement s=c.prepareStatement("UPDATE discount_codes SET used_count=GREATEST(used_count-1,0) WHERE id=?")){s.setLong(1,discountCodeId);s.executeUpdate();}}
            try(PreparedStatement s=c.prepareStatement("UPDATE invoices SET status='CANCELLED',cancelled_at=CURRENT_TIMESTAMP WHERE id=?")){s.setLong(1,id);s.executeUpdate();}
            if(customerId!=null) refreshCustomerLoyalty(c,customerId);
            c.commit();
        }catch(Exception e){c.rollback();if(e instanceof SQLException)throw(SQLException)e;throw new SQLException(e);}finally{c.setAutoCommit(true);}}
    }

    private void insertStockLog(Connection c,long productId,String type,int change,int before,int after,long invoiceId,String reason,Long userId)throws SQLException{
        try(PreparedStatement s=c.prepareStatement("INSERT INTO stock_transactions(product_id,transaction_type,quantity_change,stock_before,stock_after,reference_type,reference_id,reason,created_by) VALUES (?,?,?,?,?,'INVOICE',?,?,?)")){s.setLong(1,productId);s.setString(2,type);s.setInt(3,change);s.setInt(4,before);s.setInt(5,after);s.setLong(6,invoiceId);s.setString(7,reason);nullableLong(s,8,userId);s.executeUpdate();}
    }

    private Invoice map(ResultSet r)throws SQLException{Invoice i=new Invoice();i.setId(r.getLong("id"));i.setCode(r.getString("code"));long v=r.getLong("customer_id");i.setCustomerId(r.wasNull()?null:v);v=r.getLong("cashier_id");i.setCashierId(r.wasNull()?null:v);v=r.getLong("discount_approved_by");i.setDiscountApprovedBy(r.wasNull()?null:v);v=r.getLong("discount_code_id");i.setDiscountCodeId(r.wasNull()?null:v);i.setDiscountCode(r.getString("discount_code"));i.setCustomerName(r.getString("customer_name"));i.setCashierName(r.getString("cashier_name"));i.setDiscountApproverName(r.getString("discount_approver_name"));i.setPaymentMethod(PaymentMethod.valueOf(r.getString("payment_method")));i.setStatus(InvoiceStatus.valueOf(r.getString("status")));i.setSubtotal(r.getBigDecimal("subtotal"));i.setDiscountAmount(r.getBigDecimal("discount_amount"));i.setTotalAmount(r.getBigDecimal("total_amount"));i.setCashReceived(r.getBigDecimal("cash_received"));i.setChangeAmount(r.getBigDecimal("change_amount"));i.setNote(r.getString("note"));i.setDiscountReason(r.getString("discount_reason"));i.setCreatedAt(r.getObject("created_at",OffsetDateTime.class));i.setCancelledAt(r.getObject("cancelled_at",OffsetDateTime.class));return i;}
    private void nullableLong(PreparedStatement s,int i,Long v)throws SQLException{if(v==null)s.setNull(i,Types.BIGINT);else s.setLong(i,v);}
    private void nullableDecimal(PreparedStatement s,int i,BigDecimal v)throws SQLException{if(v==null)s.setNull(i,Types.NUMERIC);else s.setBigDecimal(i,v);}

    private void refreshCustomerLoyalty(Connection c, long customerId) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(
                "WITH stats AS (SELECT COALESCE(SUM(total_amount),0) total_spent "
                        + "FROM invoices WHERE customer_id=? AND status='PAID') "
                        + "UPDATE customers SET loyalty_points=FLOOR(stats.total_spent/10000)::INTEGER,"
                        + "customer_type=CASE WHEN stats.total_spent>=3000000 THEN 'VIP' "
                        + "WHEN stats.total_spent>=500000 THEN 'LOYAL' ELSE 'REGULAR' END "
                        + "FROM stats WHERE customers.id=?")) {
            s.setLong(1, customerId);
            s.setLong(2, customerId);
            s.executeUpdate();
        }
    }

    private static final class CustomerInfo {
        private final long id;
        private final CustomerType type;
        private CustomerInfo(long id, CustomerType type) {
            this.id = id;
            this.type = type;
        }
    }
}
