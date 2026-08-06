package com.eaut.canteen.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.eaut.canteen.dao.StockImportDAO;
import com.eaut.canteen.model.StockImport;

public class StockImportDAOImpl implements StockImportDAO {

    private static final String INSERT =
            "INSERT INTO stock_imports (admin_id, supplier_name, note) VALUES (?, ?, ?)";
    private static final String FIND_ALL =
            "SELECT i.*, u.full_name AS admin_name FROM stock_imports i " +
            "JOIN users u ON i.admin_id = u.user_id ORDER BY i.imported_at DESC";

    @Override
    public int insert(Connection conn, StockImport stockImport) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, stockImport.getAdminId());
            ps.setString(2, stockImport.getSupplierName());
            ps.setString(3, stockImport.getNote());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    @Override
    public List<StockImport> findAll(Connection conn) throws SQLException {
        List<StockImport> imports = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                StockImport stockImport = new StockImport();
                stockImport.setImportId(rs.getInt("import_id"));
                stockImport.setAdminId(rs.getInt("admin_id"));
                stockImport.setAdminName(rs.getString("admin_name"));
                stockImport.setSupplierName(rs.getString("supplier_name"));
                stockImport.setNote(rs.getString("note"));
                stockImport.setImportedAt(rs.getTimestamp("imported_at").toLocalDateTime());
                imports.add(stockImport);
            }
        }
        return imports;
    }
}
