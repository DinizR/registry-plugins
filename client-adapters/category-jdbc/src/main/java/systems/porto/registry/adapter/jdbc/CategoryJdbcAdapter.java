package systems.porto.registry.adapter.jdbc;

import systems.porto.api.sql.AbstractJdbcClientAdapter;
import systems.porto.context.Context;
import systems.porto.registry.dto.CategoryDto;
import systems.porto.registry.dto.CreateCategoryRequest;
import systems.porto.registry.dto.UpdateCategoryRequest;
import systems.porto.registry.persistence.CategoryPersistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class CategoryJdbcAdapter extends AbstractJdbcClientAdapter<Context> implements CategoryPersistence {
    private static final String LIST_CATEGORY = "LIST_CATEGORY";
    private static final String COUNT_CATEGORY = "COUNT_CATEGORY";
    private static final String READ_CATEGORY = "READ_CATEGORY";
    private static final String SEARCH_CATEGORY = "SEARCH_CATEGORY";
    private static final String INSERT_CATEGORY = "INSERT_CATEGORY";
    private static final String UPDATE_CATEGORY = "UPDATE_CATEGORY";
    private static final String DELETE_CATEGORY = "DELETE_CATEGORY";
    private static final String EXISTS_CATEGORY = "EXISTS_CATEGORY";

    @Override
    public String id() {
        return "category-jdbc";
    }

    @Override
    protected String[] requiredStatements() {
        return new String[] {
            LIST_CATEGORY, COUNT_CATEGORY, READ_CATEGORY, SEARCH_CATEGORY,
            INSERT_CATEGORY, UPDATE_CATEGORY, DELETE_CATEGORY, EXISTS_CATEGORY
        };
    }

    @Override
    public List<CategoryDto> list(final int page, final int size) {
        int offset = Math.max(page, 0) * Math.max(size, 1);
        try (Connection connection = dataSource().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql(LIST_CATEGORY))) {
            ps.setInt(1, size);
            ps.setInt(2, offset);
            return mapCategories(ps.executeQuery());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to list categories", e);
        }
    }

    @Override
    public long count() {
        try (Connection connection = dataSource().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql(COUNT_CATEGORY));
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to count categories", e);
        }
    }

    @Override
    public Optional<CategoryDto> findById(final Integer id) {
        try (Connection connection = dataSource().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql(READ_CATEGORY))) {
            ps.setInt(1, id);
            List<CategoryDto> categories = mapCategories(ps.executeQuery());
            return categories.isEmpty() ? Optional.empty() : Optional.of(categories.get(0));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read category " + id, e);
        }
    }

    @Override
    public List<CategoryDto> search(final String term) {
        if (term == null || term.isBlank()) {
            return List.of();
        }
        String like = "%" + term.toLowerCase(Locale.ROOT) + "%";
        try (Connection connection = dataSource().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql(SEARCH_CATEGORY))) {
            ps.setString(1, like);
            return mapCategories(ps.executeQuery());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to search categories", e);
        }
    }

    @Override
    public CategoryDto create(final CreateCategoryRequest request) {
        try (Connection connection = dataSource().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql(INSERT_CATEGORY), Statement.RETURN_GENERATED_KEYS)) {
            bindWrite(ps, request.name(), request.imageURL(), request.enabled(), request.barcodeDigit());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getInt(1)).orElseThrow();
                }
            }
            throw new RuntimeException("Failed to obtain generated category id");
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create category", e);
        }
    }

    @Override
    public Optional<CategoryDto> update(final Integer id, final UpdateCategoryRequest request) {
        try (Connection connection = dataSource().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql(UPDATE_CATEGORY))) {
            bindWrite(ps, request.name(), request.imageURL(), request.enabled(), request.barcodeDigit());
            ps.setInt(5, id);
            if (ps.executeUpdate() == 0) {
                return Optional.empty();
            }
            return findById(id);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update category " + id, e);
        }
    }

    @Override
    public boolean delete(final Integer id) {
        try (Connection connection = dataSource().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql(DELETE_CATEGORY))) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete category " + id, e);
        }
    }

    @Override
    public boolean existsById(final Integer id) {
        try (Connection connection = dataSource().getConnection();
             PreparedStatement ps = connection.prepareStatement(sql(EXISTS_CATEGORY))) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check category " + id, e);
        }
    }

    private void bindWrite(
        final PreparedStatement ps,
        final String name,
        final String imageUrl,
        final Boolean enabled,
        final String barcodeDigit
    ) throws SQLException {
        ps.setString(1, name);
        ps.setString(2, imageUrl != null ? imageUrl : "");
        ps.setBoolean(3, enabled == null || enabled);
        ps.setString(4, barcodeDigit != null && !barcodeDigit.isBlank() ? barcodeDigit : "0");
    }

    private List<CategoryDto> mapCategories(final ResultSet rs) throws SQLException {
        List<CategoryDto> categories = new ArrayList<>();
        while (rs.next()) {
            categories.add(CategoryDto.builder()
                .id(rs.getInt("id"))
                .name(rs.getString("name"))
                .imageURL(rs.getString("image_url"))
                .enabled(rs.getBoolean("enabled"))
                .barcodeDigit(rs.getString("barcode_digit"))
                .build());
        }
        return categories;
    }
}
