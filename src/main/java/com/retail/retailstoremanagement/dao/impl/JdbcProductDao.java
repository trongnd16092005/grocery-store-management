package com.retail.retailstoremanagement.dao.impl;

import com.retail.retailstoremanagement.dao.ProductDao;
import com.retail.retailstoremanagement.model.Product;
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

public class JdbcProductDao implements ProductDao {
    private static final String SELECT_COLUMNS =
            "SELECT p.*, c.name AS category_name, s.name AS supplier_name "
                    + "FROM products p JOIN categories c ON c.id = p.category_id "
                    + "LEFT JOIN suppliers s ON s.id = p.supplier_id ";

    @Override
    public List<Product> findAll() throws SQLException {
        return search("", null, "", 1000, 0);
    }

    @Override
    public List<Product> search(String keyword, Long categoryId, String stockStatus,
                                int limit, int offset) throws SQLException {
        QueryParts parts = buildFilters(keyword, categoryId, stockStatus);
        String sql = SELECT_COLUMNS + parts.where + " ORDER BY p.code LIMIT ? OFFSET ?";
        List<Product> products = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = bindFilters(statement, parts, 1);
            statement.setInt(index++, limit);
            statement.setInt(index, offset);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    products.add(map(resultSet));
                }
            }
        }
        return products;
    }

    @Override
    public long count(String keyword, Long categoryId, String stockStatus) throws SQLException {
        QueryParts parts = buildFilters(keyword, categoryId, stockStatus);
        String sql = "SELECT COUNT(*) FROM products p " + parts.where;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindFilters(statement, parts, 1);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    @Override
    public Optional<Product> findById(long id) throws SQLException {
        String sql = SELECT_COLUMNS + "WHERE p.id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        }
    }

    @Override
    public Product insert(Product product) throws SQLException {
        String sql = "INSERT INTO products(code, barcode, name, category_id, supplier_id, "
                + "cost_price, selling_price, stock_quantity, minimum_stock, unit, active) "
                + "VALUES ('SP' || LPAD(nextval('product_code_seq')::text, 3, '0'), ?, ?, ?, ?, ?, ?, 0, ?, ?, TRUE) "
                + "RETURNING id, code, created_at, updated_at";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableString(statement, 1, product.getBarcode());
            statement.setString(2, product.getName());
            statement.setLong(3, product.getCategoryId());
            setNullableLong(statement, 4, product.getSupplierId());
            statement.setBigDecimal(5, product.getCostPrice());
            statement.setBigDecimal(6, product.getSellingPrice());
            statement.setInt(7, product.getMinimumStock());
            statement.setString(8, product.getUnit());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                product.setId(resultSet.getLong("id"));
                product.setCode(resultSet.getString("code"));
                product.setCreatedAt(resultSet.getObject("created_at", OffsetDateTime.class));
                product.setUpdatedAt(resultSet.getObject("updated_at", OffsetDateTime.class));
                return product;
            }
        }
    }

    @Override
    public boolean update(Product product) throws SQLException {
        String sql = "UPDATE products SET barcode = ?, name = ?, category_id = ?, supplier_id = ?, "
                + "cost_price = ?, selling_price = ?, minimum_stock = ?, unit = ?, active = ? WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setNullableString(statement, 1, product.getBarcode());
            statement.setString(2, product.getName());
            statement.setLong(3, product.getCategoryId());
            setNullableLong(statement, 4, product.getSupplierId());
            statement.setBigDecimal(5, product.getCostPrice());
            statement.setBigDecimal(6, product.getSellingPrice());
            statement.setInt(7, product.getMinimumStock());
            statement.setString(8, product.getUnit());
            statement.setBoolean(9, product.isActive());
            statement.setLong(10, product.getId());
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean barcodeExists(String barcode, Long excludedId) throws SQLException {
        if (barcode == null || barcode.isBlank()) {
            return false;
        }
        String sql = "SELECT EXISTS(SELECT 1 FROM products WHERE barcode = ? AND (? IS NULL OR id <> ?))";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, barcode);
            setNullableLong(statement, 2, excludedId);
            setNullableLong(statement, 3, excludedId);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getBoolean(1);
            }
        }
    }

    @Override
    public boolean softDelete(long id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE products SET active = FALSE WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    private QueryParts buildFilters(String keyword, Long categoryId, String stockStatus) {
        StringBuilder where = new StringBuilder("WHERE p.active ");
        List<Object> parameters = new ArrayList<>();
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
        if (!normalized.isEmpty()) {
            where.append("AND (LOWER(p.code) LIKE ? OR LOWER(p.name) LIKE ? OR LOWER(COALESCE(p.barcode, '')) LIKE ?) ");
            String like = "%" + normalized + "%";
            parameters.add(like);
            parameters.add(like);
            parameters.add(like);
        }
        if (categoryId != null) {
            where.append("AND p.category_id = ? ");
            parameters.add(categoryId);
        }
        if ("OUT".equalsIgnoreCase(stockStatus)) {
            where.append("AND p.stock_quantity = 0 ");
        } else if ("LOW".equalsIgnoreCase(stockStatus)) {
            where.append("AND p.stock_quantity > 0 AND p.stock_quantity <= p.minimum_stock ");
        } else if ("OK".equalsIgnoreCase(stockStatus)) {
            where.append("AND p.stock_quantity > p.minimum_stock ");
        }
        return new QueryParts(where.toString(), parameters);
    }

    private int bindFilters(PreparedStatement statement, QueryParts parts, int startIndex)
            throws SQLException {
        int index = startIndex;
        for (Object parameter : parts.parameters) {
            if (parameter instanceof Long) {
                statement.setLong(index++, (Long) parameter);
            } else {
                statement.setString(index++, parameter.toString());
            }
        }
        return index;
    }

    private Product map(ResultSet resultSet) throws SQLException {
        Product product = new Product();
        product.setId(resultSet.getLong("id"));
        product.setCode(resultSet.getString("code"));
        product.setBarcode(resultSet.getString("barcode"));
        product.setName(resultSet.getString("name"));
        product.setCategoryId(resultSet.getLong("category_id"));
        long supplierId = resultSet.getLong("supplier_id");
        product.setSupplierId(resultSet.wasNull() ? null : supplierId);
        product.setCategoryName(resultSet.getString("category_name"));
        product.setSupplierName(resultSet.getString("supplier_name"));
        product.setCostPrice(resultSet.getBigDecimal("cost_price"));
        product.setSellingPrice(resultSet.getBigDecimal("selling_price"));
        product.setStockQuantity(resultSet.getInt("stock_quantity"));
        product.setMinimumStock(resultSet.getInt("minimum_stock"));
        product.setUnit(resultSet.getString("unit"));
        product.setActive(resultSet.getBoolean("active"));
        product.setCreatedAt(resultSet.getObject("created_at", OffsetDateTime.class));
        product.setUpdatedAt(resultSet.getObject("updated_at", OffsetDateTime.class));
        return product;
    }

    private void setNullableLong(PreparedStatement statement, int index, Long value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.BIGINT);
        } else {
            statement.setLong(index, value);
        }
    }

    private void setNullableString(PreparedStatement statement, int index, String value)
            throws SQLException {
        if (value == null || value.isBlank()) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    private static final class QueryParts {
        private final String where;
        private final List<Object> parameters;

        private QueryParts(String where, List<Object> parameters) {
            this.where = where;
            this.parameters = parameters;
        }
    }
}
