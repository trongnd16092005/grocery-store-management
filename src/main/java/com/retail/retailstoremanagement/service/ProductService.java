package com.retail.retailstoremanagement.service;

import com.retail.retailstoremanagement.dao.ProductDao;
import com.retail.retailstoremanagement.dao.impl.JdbcProductDao;
import com.retail.retailstoremanagement.model.Product;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class ProductService {
    private final ProductDao productDao;

    public ProductService() {
        this(new JdbcProductDao());
    }

    public ProductService(ProductDao productDao) {
        this.productDao = productDao;
    }

    public List<Product> search(String keyword, Long categoryId, String stockStatus,
                                int page, int pageSize) throws SQLException {
        int safePage = Math.max(page, 1);
        return productDao.search(
                keyword, categoryId, stockStatus, pageSize, (safePage - 1) * pageSize
        );
    }

    public long count(String keyword, Long categoryId, String stockStatus) throws SQLException {
        return productDao.count(keyword, categoryId, stockStatus);
    }

    public List<Product> findAll() throws SQLException {
        return productDao.findAll();
    }

    public Product findById(long id) throws SQLException {
        return productDao.findById(id)
                .orElseThrow(() -> new ValidationException("Không tìm thấy sản phẩm."));
    }

    public Product save(Product product) throws SQLException {
        validate(product);
        if (productDao.barcodeExists(product.getBarcode(), product.getId())) {
            throw new ValidationException("Mã vạch đã được sử dụng bởi sản phẩm khác.");
        }

        if (product.getId() == null) {
            product.setStockQuantity(0);
            return productDao.insert(product);
        }

        Product existing = findById(product.getId());
        product.setStockQuantity(existing.getStockQuantity());
        product.setActive(existing.isActive());
        if (!productDao.update(product)) {
            throw new ValidationException("Không thể cập nhật sản phẩm.");
        }
        return product;
    }

    public void delete(long id) throws SQLException {
        findById(id);
        if (!productDao.softDelete(id)) {
            throw new ValidationException("Không thể ngừng kinh doanh sản phẩm.");
        }
    }

    private void validate(Product product) {
        if (product.getName() == null || product.getName().isBlank()) {
            throw new ValidationException("Tên sản phẩm không được để trống.");
        }
        if (product.getCategoryId() == null) {
            throw new ValidationException("Vui lòng chọn danh mục.");
        }
        if (product.getCostPrice() == null
                || product.getCostPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Giá nhập không được âm.");
        }
        if (product.getSellingPrice() == null
                || product.getSellingPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Giá bán phải lớn hơn 0.");
        }
        if (product.getMinimumStock() < 0) {
            throw new ValidationException("Tồn tối thiểu không được âm.");
        }
        if (product.getUnit() == null || product.getUnit().isBlank()) {
            throw new ValidationException("Đơn vị tính không được để trống.");
        }
    }
}
