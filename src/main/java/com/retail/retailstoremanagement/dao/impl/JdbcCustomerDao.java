package com.retail.retailstoremanagement.dao.impl;

import com.retail.retailstoremanagement.dao.CustomerDao;
import com.retail.retailstoremanagement.model.Customer;
import com.retail.retailstoremanagement.model.CustomerType;
import com.retail.retailstoremanagement.model.Gender;
import com.retail.retailstoremanagement.model.Invoice;
import com.retail.retailstoremanagement.model.InvoiceStatus;
import com.retail.retailstoremanagement.model.PaymentMethod;
import com.retail.retailstoremanagement.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcCustomerDao implements CustomerDao {
    private static final String SELECT_BASE =
            "SELECT c.*, COUNT(i.id) AS purchase_count, "
                    + "COALESCE(SUM(i.total_amount), 0) AS total_spent, "
                    + "MAX(i.created_at) AS last_purchase_at "
                    + "FROM customers c LEFT JOIN invoices i "
                    + "ON i.customer_id = c.id AND i.status = 'PAID' ";

    @Override
    public List<Customer> findAll() throws SQLException {
        return search("", "");
    }

    @Override
    public List<Customer> search(String keyword, String customerType) throws SQLException {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
        String type = customerType == null ? "" : customerType.trim().toUpperCase();
        String sql = SELECT_BASE
                + "WHERE c.active AND (? = '' OR LOWER(c.code) LIKE ? OR LOWER(c.full_name) LIKE ? "
                + "OR REPLACE(c.phone, ' ', '') LIKE REPLACE(?, ' ', '')) "
                + "AND (? = '' OR c.customer_type = ?) "
                + "GROUP BY c.id ORDER BY c.code";
        String like = "%" + normalized + "%";
        List<Customer> customers = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalized);
            statement.setString(2, like);
            statement.setString(3, like);
            statement.setString(4, like);
            statement.setString(5, type);
            statement.setString(6, type);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    customers.add(map(resultSet));
                }
            }
        }
        return customers;
    }

    @Override
    public Optional<Customer> findById(long id) throws SQLException {
        String sql = SELECT_BASE + "WHERE c.id = ? GROUP BY c.id";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    @Override
    public Optional<Customer> findByCode(String code) throws SQLException {
        String sql = SELECT_BASE + "WHERE c.active AND UPPER(c.code) = UPPER(?) GROUP BY c.id";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Invoice> purchaseHistory(long customerId, int limit) throws SQLException {
        String sql = "SELECT * FROM invoices WHERE customer_id=? "
                + "ORDER BY created_at DESC LIMIT ?";
        List<Invoice> invoices = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            statement.setInt(2, Math.max(1, limit));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) invoices.add(mapInvoice(resultSet));
            }
        }
        return invoices;
    }

    @Override
    public Customer insert(Customer customer) throws SQLException {
        String sql = "INSERT INTO customers(code, full_name, phone, email, gender, address, customer_type, active) "
                + "VALUES ('KH' || LPAD(nextval('customer_code_seq')::text, 3, '0'), ?, ?, ?, ?, ?, ?, TRUE) "
                + "RETURNING id, code, created_at, updated_at";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindCustomer(statement, customer, false);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                customer.setId(resultSet.getLong("id"));
                customer.setCode(resultSet.getString("code"));
                customer.setCreatedAt(resultSet.getObject("created_at", OffsetDateTime.class));
                customer.setUpdatedAt(resultSet.getObject("updated_at", OffsetDateTime.class));
                return customer;
            }
        }
    }

    @Override
    public boolean update(Customer customer) throws SQLException {
        String sql = "UPDATE customers SET full_name = ?, phone = ?, email = ?, gender = ?, "
                + "address = ?, customer_type = ?, active = ? WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindCustomer(statement, customer, true);
            statement.setLong(8, customer.getId());
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean phoneExists(String phone, Long excludedId) throws SQLException {
        String sql = "SELECT EXISTS(SELECT 1 FROM customers WHERE phone = ? AND (? IS NULL OR id <> ?))";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, phone);
            setNullableLong(statement, 2, excludedId);
            setNullableLong(statement, 3, excludedId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBoolean(1);
            }
        }
    }

    private void bindCustomer(PreparedStatement statement, Customer customer, boolean includeActive)
            throws SQLException {
        statement.setString(1, customer.getFullName());
        statement.setString(2, customer.getPhone());
        setNullableString(statement, 3, customer.getEmail());
        setNullableString(statement, 4,
                customer.getGender() == null ? null : customer.getGender().name());
        setNullableString(statement, 5, customer.getAddress());
        statement.setString(6, customer.getCustomerType().name());
        if (includeActive) {
            statement.setBoolean(7, customer.isActive());
        }
    }

    private Customer map(ResultSet resultSet) throws SQLException {
        Customer customer = new Customer();
        customer.setId(resultSet.getLong("id"));
        customer.setCode(resultSet.getString("code"));
        customer.setFullName(resultSet.getString("full_name"));
        customer.setPhone(resultSet.getString("phone"));
        customer.setEmail(resultSet.getString("email"));
        String gender = resultSet.getString("gender");
        customer.setGender(gender == null ? null : Gender.valueOf(gender));
        customer.setAddress(resultSet.getString("address"));
        customer.setCustomerType(CustomerType.valueOf(resultSet.getString("customer_type")));
        customer.setActive(resultSet.getBoolean("active"));
        customer.setLoyaltyPoints(resultSet.getInt("loyalty_points"));
        customer.setCreatedAt(resultSet.getObject("created_at", OffsetDateTime.class));
        customer.setUpdatedAt(resultSet.getObject("updated_at", OffsetDateTime.class));
        customer.setPurchaseCount(resultSet.getLong("purchase_count"));
        BigDecimal totalSpent = resultSet.getBigDecimal("total_spent");
        customer.setTotalSpent(totalSpent == null ? BigDecimal.ZERO : totalSpent);
        customer.setLastPurchaseAt(resultSet.getObject("last_purchase_at", OffsetDateTime.class));
        return customer;
    }

    private Invoice mapInvoice(ResultSet resultSet) throws SQLException {
        Invoice invoice = new Invoice();
        invoice.setId(resultSet.getLong("id"));
        invoice.setCode(resultSet.getString("code"));
        invoice.setPaymentMethod(PaymentMethod.valueOf(resultSet.getString("payment_method")));
        invoice.setStatus(InvoiceStatus.valueOf(resultSet.getString("status")));
        invoice.setSubtotal(resultSet.getBigDecimal("subtotal"));
        invoice.setDiscountAmount(resultSet.getBigDecimal("discount_amount"));
        invoice.setTotalAmount(resultSet.getBigDecimal("total_amount"));
        invoice.setDiscountCode(resultSet.getString("discount_code"));
        invoice.setCreatedAt(resultSet.getObject("created_at", OffsetDateTime.class));
        invoice.setCancelledAt(resultSet.getObject("cancelled_at", OffsetDateTime.class));
        return invoice;
    }

    private void setNullableString(PreparedStatement statement, int index, String value)
            throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    private void setNullableLong(PreparedStatement statement, int index, Long value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }
}
