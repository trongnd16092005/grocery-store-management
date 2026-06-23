package com.retail.retailstoremanagement.dao.impl;

import com.retail.retailstoremanagement.dao.CategoryDao;
import com.retail.retailstoremanagement.model.Category;
import com.retail.retailstoremanagement.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcCategoryDao implements CategoryDao {
    private static final String SELECT_BASE =
            "SELECT c.*, COUNT(p.id) FILTER (WHERE p.active) AS product_count "
                    + "FROM categories c LEFT JOIN products p ON p.category_id = c.id ";

    @Override
    public List<Category> findAll() throws SQLException {
        return search("");
    }

    @Override
    public List<Category> search(String keyword) throws SQLException {
        String sql = SELECT_BASE
                + "WHERE c.active AND (? = '' OR LOWER(c.code) LIKE ? OR LOWER(c.name) LIKE ? "
                + "OR LOWER(COALESCE(c.description, '')) LIKE ?) "
                + "GROUP BY c.id ORDER BY c.name";
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
        String like = "%" + normalized + "%";
        List<Category> categories = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, normalized);
            statement.setString(2, like);
            statement.setString(3, like);
            statement.setString(4, like);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    categories.add(map(resultSet));
                }
            }
        }
        return categories;
    }

    @Override
    public Optional<Category> findById(long id) throws SQLException {
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
    public Category insert(Category category) throws SQLException {
        String sql = "INSERT INTO categories(code, name, description, active) "
                + "VALUES ('DM' || LPAD(nextval('category_code_seq')::text, 3, '0'), ?, ?, TRUE) "
                + "RETURNING id, code, created_at, updated_at";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category.getName());
            statement.setString(2, category.getDescription());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                category.setId(resultSet.getLong("id"));
                category.setCode(resultSet.getString("code"));
                category.setCreatedAt(resultSet.getObject("created_at", OffsetDateTime.class));
                category.setUpdatedAt(resultSet.getObject("updated_at", OffsetDateTime.class));
                return category;
            }
        }
    }

    @Override
    public boolean update(Category category) throws SQLException {
        String sql = "UPDATE categories SET name = ?, description = ?, active = ? WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, category.getName());
            statement.setString(2, category.getDescription());
            statement.setBoolean(3, category.isActive());
            statement.setLong(4, category.getId());
            return statement.executeUpdate() == 1;
        }
    }

    @Override
    public boolean nameExists(String name, Long excludedId) throws SQLException {
        String sql = "SELECT EXISTS(SELECT 1 FROM categories WHERE LOWER(name) = LOWER(?) "
                + "AND (? IS NULL OR id <> ?))";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            if (excludedId == null) {
                statement.setNull(2, java.sql.Types.BIGINT);
                statement.setNull(3, java.sql.Types.BIGINT);
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

    @Override
    public boolean softDelete(long id) throws SQLException {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE categories SET active = FALSE WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    private Category map(ResultSet resultSet) throws SQLException {
        Category category = new Category();
        category.setId(resultSet.getLong("id"));
        category.setCode(resultSet.getString("code"));
        category.setName(resultSet.getString("name"));
        category.setDescription(resultSet.getString("description"));
        category.setActive(resultSet.getBoolean("active"));
        category.setCreatedAt(resultSet.getObject("created_at", OffsetDateTime.class));
        category.setUpdatedAt(resultSet.getObject("updated_at", OffsetDateTime.class));
        category.setProductCount(resultSet.getLong("product_count"));
        return category;
    }
}
