package com.retail.retailstoremanagement.dao.impl;

import com.retail.retailstoremanagement.dao.DiscountCodeDao;
import com.retail.retailstoremanagement.model.DiscountCode;
import com.retail.retailstoremanagement.model.DiscountType;
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

public class JdbcDiscountCodeDao implements DiscountCodeDao {
    @Override
    public List<DiscountCode> findAll(boolean includeInactive) throws SQLException {
        List<DiscountCode> codes = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM discount_codes WHERE (? OR active) "
                             + "ORDER BY active DESC, created_at DESC")) {
            statement.setBoolean(1, includeInactive);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) codes.add(map(resultSet));
            }
        }
        return codes;
    }

    @Override
    public Optional<DiscountCode> findById(long id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement("SELECT * FROM discount_codes WHERE id=?")) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    @Override
    public Optional<DiscountCode> findByCode(String code) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return findByCode(connection, code, false);
        }
    }

    @Override
    public Optional<DiscountCode> lockByCode(Connection connection, String code)
            throws SQLException {
        return findByCode(connection, code, true);
    }

    private Optional<DiscountCode> findByCode(Connection connection, String code, boolean lock)
            throws SQLException {
        String sql = "SELECT * FROM discount_codes WHERE UPPER(code)=UPPER(?)"
                + (lock ? " FOR UPDATE" : "");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, code);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    @Override
    public DiscountCode insert(DiscountCode code) throws SQLException {
        String sql = "INSERT INTO discount_codes(code,name,discount_type,discount_value,"
                + "minimum_order,maximum_discount,starts_at,ends_at,usage_limit,active) "
                + "VALUES (UPPER(?),?,?,?,?,?,?,?,?,TRUE) RETURNING *";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, code);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return map(resultSet);
            }
        }
    }

    @Override
    public boolean update(DiscountCode code) throws SQLException {
        String sql = "UPDATE discount_codes SET code=UPPER(?),name=?,discount_type=?,"
                + "discount_value=?,minimum_order=?,maximum_discount=?,starts_at=?,ends_at=?,"
                + "usage_limit=? WHERE id=?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, code);
            statement.setLong(10, code.getId());
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean setActive(long id, boolean active) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE discount_codes SET active=? WHERE id=?")) {
            statement.setBoolean(1, active);
            statement.setLong(2, id);
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public void incrementUsage(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE discount_codes SET used_count=used_count+1 WHERE id=?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    private void bind(PreparedStatement statement, DiscountCode code) throws SQLException {
        statement.setString(1, code.getCode());
        statement.setString(2, code.getName());
        statement.setString(3, code.getDiscountType().name());
        statement.setBigDecimal(4, code.getDiscountValue());
        statement.setBigDecimal(5, code.getMinimumOrder());
        setNullableDecimal(statement, 6, code.getMaximumDiscount());
        setNullableTimestamp(statement, 7, code.getStartsAt());
        setNullableTimestamp(statement, 8, code.getEndsAt());
        if (code.getUsageLimit() == null) statement.setNull(9, Types.INTEGER);
        else statement.setInt(9, code.getUsageLimit());
    }

    private DiscountCode map(ResultSet resultSet) throws SQLException {
        DiscountCode code = new DiscountCode();
        code.setId(resultSet.getLong("id"));
        code.setCode(resultSet.getString("code"));
        code.setName(resultSet.getString("name"));
        code.setDiscountType(DiscountType.valueOf(resultSet.getString("discount_type")));
        code.setDiscountValue(resultSet.getBigDecimal("discount_value"));
        code.setMinimumOrder(resultSet.getBigDecimal("minimum_order"));
        code.setMaximumDiscount(resultSet.getBigDecimal("maximum_discount"));
        code.setStartsAt(resultSet.getObject("starts_at", OffsetDateTime.class));
        code.setEndsAt(resultSet.getObject("ends_at", OffsetDateTime.class));
        int usageLimit = resultSet.getInt("usage_limit");
        code.setUsageLimit(resultSet.wasNull() ? null : usageLimit);
        code.setUsedCount(resultSet.getInt("used_count"));
        code.setActive(resultSet.getBoolean("active"));
        code.setCreatedAt(resultSet.getObject("created_at", OffsetDateTime.class));
        code.setUpdatedAt(resultSet.getObject("updated_at", OffsetDateTime.class));
        return code;
    }

    private void setNullableDecimal(PreparedStatement statement, int index,
                                    java.math.BigDecimal value) throws SQLException {
        if (value == null) statement.setNull(index, Types.NUMERIC);
        else statement.setBigDecimal(index, value);
    }

    private void setNullableTimestamp(PreparedStatement statement, int index,
                                      OffsetDateTime value) throws SQLException {
        if (value == null) statement.setNull(index, Types.TIMESTAMP_WITH_TIMEZONE);
        else statement.setObject(index, value);
    }
}
