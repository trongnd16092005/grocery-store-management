package com.retail.retailstoremanagement.service;

import com.retail.retailstoremanagement.dao.CategoryDao;
import com.retail.retailstoremanagement.dao.impl.JdbcCategoryDao;
import com.retail.retailstoremanagement.model.Category;

import java.sql.SQLException;
import java.util.List;

public class CategoryService {
    private final CategoryDao categoryDao;

    public CategoryService() {
        this(new JdbcCategoryDao());
    }

    public CategoryService(CategoryDao categoryDao) {
        this.categoryDao = categoryDao;
    }

    public List<Category> search(String keyword) throws SQLException {
        return categoryDao.search(keyword);
    }

    public List<Category> findAll() throws SQLException {
        return categoryDao.findAll();
    }

    public Category findById(long id) throws SQLException {
        return categoryDao.findById(id)
                .orElseThrow(() -> new ValidationException("Không tìm thấy danh mục."));
    }

    public Category save(Category category) throws SQLException {
        validate(category);
        if (categoryDao.nameExists(category.getName(), category.getId())) {
            throw new ValidationException("Tên danh mục đã tồn tại.");
        }
        if (category.getId() == null) {
            return categoryDao.insert(category);
        }
        if (!categoryDao.update(category)) {
            throw new ValidationException("Không thể cập nhật danh mục.");
        }
        return category;
    }

    public void delete(long id) throws SQLException {
        Category category = findById(id);
        if (category.getProductCount() > 0) {
            throw new ValidationException(
                    "Không thể xóa danh mục đang có " + category.getProductCount() + " sản phẩm."
            );
        }
        if (!categoryDao.softDelete(id)) {
            throw new ValidationException("Không thể xóa danh mục.");
        }
    }

    private void validate(Category category) {
        if (category.getName() == null || category.getName().isBlank()) {
            throw new ValidationException("Tên danh mục không được để trống.");
        }
        if (category.getName().length() > 100) {
            throw new ValidationException("Tên danh mục không được vượt quá 100 ký tự.");
        }
    }
}
