package com.retail.retailstoremanagement.dao.impl;

import com.retail.retailstoremanagement.dao.SupplierDao;
import com.retail.retailstoremanagement.model.Supplier;
import com.retail.retailstoremanagement.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcSupplierDao implements SupplierDao {
    @Override
    public List<Supplier> findAll() throws SQLException {
        List<Supplier> suppliers = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM suppliers WHERE active ORDER BY name");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                suppliers.add(map(resultSet));
            }
        }
        return suppliers;
    }

    @Override
    public Optional<Supplier> findById(long id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM suppliers WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    @Override
    public Supplier insert(Supplier supplier) throws SQLException {
        String sql = "INSERT INTO suppliers(code, name, phone, email, address) "
                + "VALUES ('NCC' || LPAD(nextval('supplier_code_seq')::text, 3, '0'), ?, ?, ?, ?) "
                + "RETURNING id, code";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, supplier.getName());
            statement.setString(2, supplier.getPhone());
            statement.setString(3, supplier.getEmail());
            statement.setString(4, supplier.getAddress());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                supplier.setId(resultSet.getLong("id"));
                supplier.setCode(resultSet.getString("code"));
                return supplier;
            }
        }
    }

    @Override
    public boolean update(Supplier supplier) throws SQLException {
        String sql = "UPDATE suppliers SET name = ?, phone = ?, email = ?, address = ?, active = ? WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, supplier.getName());
            statement.setString(2, supplier.getPhone());
            statement.setString(3, supplier.getEmail());
            statement.setString(4, supplier.getAddress());
            statement.setBoolean(5, supplier.isActive());
            statement.setLong(6, supplier.getId());
            return statement.executeUpdate() == 1;
        }
    }

    private Supplier map(ResultSet resultSet) throws SQLException {
        Supplier supplier = new Supplier();
        supplier.setId(resultSet.getLong("id"));
        supplier.setCode(resultSet.getString("code"));
        supplier.setName(resultSet.getString("name"));
        supplier.setPhone(resultSet.getString("phone"));
        supplier.setEmail(resultSet.getString("email"));
        supplier.setAddress(resultSet.getString("address"));
        supplier.setActive(resultSet.getBoolean("active"));
        supplier.setCreatedAt(resultSet.getObject("created_at", OffsetDateTime.class));
        supplier.setUpdatedAt(resultSet.getObject("updated_at", OffsetDateTime.class));
        return supplier;
    }
}
