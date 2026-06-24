package com.retail.retailstoremanagement.dao.impl;

import com.retail.retailstoremanagement.dao.SupplierDao;
import com.retail.retailstoremanagement.model.Supplier;
import com.retail.retailstoremanagement.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcSupplierDao implements SupplierDao {
    private static final String SELECT_WITH_PRODUCT_COUNT =
            "SELECT s.*, COUNT(p.id) FILTER (WHERE p.active) AS product_count "
                    + "FROM suppliers s LEFT JOIN products p ON p.supplier_id=s.id ";

    @Override
    public List<Supplier> findAll() throws SQLException {
        return search("", false);
    }

    @Override
    public List<Supplier> search(String keyword, boolean includeInactive) throws SQLException {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        String sql = SELECT_WITH_PRODUCT_COUNT
                + "WHERE (? OR s.active) "
                + "AND (?='' OR LOWER(s.code) LIKE ? OR LOWER(s.name) LIKE ? "
                + "OR LOWER(COALESCE(s.phone,'')) LIKE ? OR LOWER(COALESCE(s.email,'')) LIKE ?) "
                + "GROUP BY s.id ORDER BY s.active DESC, s.name";
        List<Supplier> suppliers = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String pattern = "%" + normalizedKeyword + "%";
            statement.setBoolean(1, includeInactive);
            statement.setString(2, normalizedKeyword);
            statement.setString(3, pattern);
            statement.setString(4, pattern);
            statement.setString(5, pattern);
            statement.setString(6, pattern);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) suppliers.add(map(resultSet));
            }
        }
        return suppliers;
    }

    @Override
    public Optional<Supplier> findById(long id) throws SQLException {
        String sql = SELECT_WITH_PRODUCT_COUNT + "WHERE s.id=? GROUP BY s.id";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    @Override
    public Supplier insert(Supplier supplier) throws SQLException {
        String sql = "INSERT INTO suppliers(code,name,phone,email,address,active) "
                + "VALUES ('NCC'||LPAD(nextval('supplier_code_seq')::text,3,'0'),?,?,?,?,TRUE) "
                + "RETURNING *";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, supplier);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return mapBasic(resultSet, supplier);
            }
        }
    }

    @Override
    public boolean update(Supplier supplier) throws SQLException {
        String sql = "UPDATE suppliers SET name=?,phone=?,email=?,address=? WHERE id=?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, supplier);
            statement.setLong(5, supplier.getId());
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean setActive(long id, boolean active) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE suppliers SET active=? WHERE id=?")) {
            statement.setBoolean(1, active);
            statement.setLong(2, id);
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean nameExists(String name, Long excludedId) throws SQLException {
        String sql = "SELECT EXISTS(SELECT 1 FROM suppliers "
                + "WHERE LOWER(name)=LOWER(?) AND (? IS NULL OR id<>?))";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            if (excludedId == null) {
                statement.setNull(2, Types.BIGINT);
                statement.setNull(3, Types.BIGINT);
            } else {
                statement.setLong(2, excludedId);
                statement.setLong(3, excludedId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBoolean(1);
            }
        }
    }

    private void bind(PreparedStatement statement, Supplier supplier) throws SQLException {
        statement.setString(1, supplier.getName());
        setNullable(statement, 2, supplier.getPhone());
        setNullable(statement, 3, supplier.getEmail());
        setNullable(statement, 4, supplier.getAddress());
    }

    private void setNullable(PreparedStatement statement, int index, String value)
            throws SQLException {
        if (value == null || value.isBlank()) statement.setNull(index, Types.VARCHAR);
        else statement.setString(index, value);
    }

    private Supplier mapBasic(ResultSet resultSet, Supplier supplier) throws SQLException {
        supplier.setId(resultSet.getLong("id"));
        supplier.setCode(resultSet.getString("code"));
        supplier.setActive(resultSet.getBoolean("active"));
        supplier.setCreatedAt(resultSet.getObject("created_at", OffsetDateTime.class));
        supplier.setUpdatedAt(resultSet.getObject("updated_at", OffsetDateTime.class));
        return supplier;
    }

    private Supplier map(ResultSet resultSet) throws SQLException {
        Supplier supplier = new Supplier();
        supplier.setName(resultSet.getString("name"));
        supplier.setPhone(resultSet.getString("phone"));
        supplier.setEmail(resultSet.getString("email"));
        supplier.setAddress(resultSet.getString("address"));
        mapBasic(resultSet, supplier);
        supplier.setProductCount(resultSet.getLong("product_count"));
        return supplier;
    }
}
