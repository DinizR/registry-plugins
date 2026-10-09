package systems.porto.registry.adapter.jdbc;

import systems.porto.api.sql.AbstractJdbcClientAdapter;
import systems.porto.context.Context;
import systems.porto.registry.dto.CreateProductRequest;
import systems.porto.registry.dto.ProductDto;
import systems.porto.registry.dto.UpdateProductRequest;
import systems.porto.registry.persistence.ProductPersistence;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ProductJdbcAdapter extends AbstractJdbcClientAdapter<Context> implements ProductPersistence {
    private static final String LIST_PRODUCT = "LIST_PRODUCT";
    private static final String COUNT_PRODUCT = "COUNT_PRODUCT";
    private static final String READ_PRODUCT = "READ_PRODUCT";
    private static final String SEARCH_PRODUCT = "SEARCH_PRODUCT";
    private static final String INSERT_PRODUCT = "INSERT_PRODUCT";
    private static final String UPDATE_PRODUCT = "UPDATE_PRODUCT";
    private static final String DELETE_PRODUCT = "DELETE_PRODUCT";
    private static final String EXISTS_CATEGORY = "EXISTS_CATEGORY";

    @Override
    public String id() {
        return "product-jdbc";
    }

    @Override
    protected String[] requiredStatements() {
        return new String[] {
            LIST_PRODUCT, COUNT_PRODUCT, READ_PRODUCT, SEARCH_PRODUCT,
            INSERT_PRODUCT, UPDATE_PRODUCT, DELETE_PRODUCT, EXISTS_CATEGORY
        };
    }

    @Override
    public List<ProductDto> list(final int page, final int size) {
        int offset = Math.max(page, 0) * Math.max(size, 1);
        try (Connection connection = dataSource().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql(LIST_PRODUCT))) {
            ps.setInt(1, size);
            ps.setInt(2, offset);
            return mapProducts(ps.executeQuery());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list products", e);
        }
    }

    @Override
    public long count() {
        try (Connection connection = dataSource().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql(COUNT_PRODUCT));
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count products", e);
        }
    }

    @Override
    public Optional<ProductDto> findById(final Integer id) {
        try (Connection connection = dataSource().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql(READ_PRODUCT))) {
            ps.setInt(1, id);
            List<ProductDto> products = mapProducts(ps.executeQuery());
            return products.isEmpty() ? Optional.empty() : Optional.of(products.get(0));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read product " + id, e);
        }
    }

    @Override
    public List<ProductDto> search(final String term) {
        if (term == null || term.isBlank()) {
            return List.of();
        }
        String like = "%" + term.toLowerCase(Locale.ROOT) + "%";
        try (Connection connection = dataSource().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql(SEARCH_PRODUCT))) {
            ps.setString(1, term.trim());
            ps.setString(2, like);
            return mapProducts(ps.executeQuery());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search products", e);
        }
    }

    @Override
    public ProductDto create(final CreateProductRequest request) {
        requireCategoryExists(request.categoryId());
        try (Connection connection = dataSource().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql(INSERT_PRODUCT), Statement.RETURN_GENERATED_KEYS)) {
            bindWrite(ps, request.barcode(), request.description(), request.unitPrice(), request.discount(),
                request.taxRate(), request.unit(), request.imageURL(), request.categoryId(), request.enabled());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getInt(1)).orElseThrow();
                }
            }
            throw new RuntimeException("Failed to obtain generated product id");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create product", e);
        }
    }

    @Override
    public Optional<ProductDto> update(final Integer id, final UpdateProductRequest request) {
        requireCategoryExists(request.categoryId());
        try (Connection connection = dataSource().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql(UPDATE_PRODUCT))) {
            bindWrite(ps, request.barcode(), request.description(), request.unitPrice(), request.discount(),
                request.taxRate(), request.unit(), request.imageURL(), request.categoryId(), request.enabled());
            ps.setInt(10, id);
            if (ps.executeUpdate() == 0) {
                return Optional.empty();
            }
            return findById(id);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update product " + id, e);
        }
    }

    @Override
    public boolean delete(final Integer id) {
        try (Connection connection = dataSource().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql(DELETE_PRODUCT))) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete product " + id, e);
        }
    }

    private void requireCategoryExists(final int categoryId) {
        try (Connection connection = dataSource().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql(EXISTS_CATEGORY))) {
            ps.setInt(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Category not found: " + categoryId);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to validate category " + categoryId, e);
        }
    }

    private void bindWrite(
        final PreparedStatement ps,
        final String barcode,
        final String description,
        final BigDecimal unitPrice,
        final BigDecimal discount,
        final BigDecimal taxRate,
        final String unit,
        final String imageUrl,
        final Integer categoryId,
        final Boolean enabled
    ) throws SQLException {
        ps.setString(1, barcode);
        ps.setString(2, description);
        ps.setBigDecimal(3, unitPrice);
        ps.setBigDecimal(4, discount != null ? discount : BigDecimal.ZERO);
        ps.setBigDecimal(5, taxRate != null ? taxRate : BigDecimal.ZERO);
        ps.setString(6, unit);
        ps.setString(7, imageUrl != null ? imageUrl : "");
        ps.setInt(8, categoryId);
        ps.setBoolean(9, enabled == null || enabled);
    }

    private List<ProductDto> mapProducts(final ResultSet rs) throws SQLException {
        List<ProductDto> products = new ArrayList<>();
        while (rs.next()) {
            products.add(ProductDto.builder()
                .id(rs.getInt("id"))
                .barcode(rs.getString("barcode"))
                .description(rs.getString("description"))
                .unitPrice(rs.getBigDecimal("unit_price"))
                .discount(rs.getBigDecimal("discount"))
                .taxRate(rs.getBigDecimal("tax_rate"))
                .unit(rs.getString("unit"))
                .imageURL(rs.getString("image_url"))
                .categoryId(rs.getInt("category_id"))
                .enabled(rs.getBoolean("enabled"))
                .build());
        }
        return products;
    }
}
