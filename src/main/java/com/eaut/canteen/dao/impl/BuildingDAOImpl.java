package com.eaut.canteen.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.eaut.canteen.dao.BuildingDAO;
import com.eaut.canteen.model.Building;

public class BuildingDAOImpl implements BuildingDAO {

    private static final String FIND_ALL_ACTIVE =
            "SELECT * FROM buildings WHERE is_active = TRUE ORDER BY name";
    private static final String FIND_ALL =
            "SELECT * FROM buildings ORDER BY name";
    private static final String FIND_BY_ID =
            "SELECT * FROM buildings WHERE building_id = ?";
    private static final String INSERT =
            "INSERT INTO buildings (name, description, shipping_fee) VALUES (?, ?, ?)";
    private static final String UPDATE =
            "UPDATE buildings SET name = ?, description = ?, shipping_fee = ? WHERE building_id = ?";
    private static final String SET_ACTIVE =
            "UPDATE buildings SET is_active = ? WHERE building_id = ?";

    @Override
    public List<Building> findAllActive(Connection conn) throws SQLException {
        return query(conn, FIND_ALL_ACTIVE);
    }

    @Override
    public List<Building> findAll(Connection conn) throws SQLException {
        return query(conn, FIND_ALL);
    }

    private List<Building> query(Connection conn, String sql) throws SQLException {
        List<Building> buildings = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                buildings.add(mapRow(rs));
            }
        }
        return buildings;
    }

    @Override
    public Building findById(Connection conn, int buildingId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(FIND_BY_ID)) {
            ps.setInt(1, buildingId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    @Override
    public void insert(Connection conn, Building building) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, building.getName());
            ps.setString(2, building.getDescription());
            ps.setBigDecimal(3, building.getShippingFee());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                building.setBuildingId(keys.getInt(1));
            }
        }
    }

    @Override
    public void update(Connection conn, Building building) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            ps.setString(1, building.getName());
            ps.setString(2, building.getDescription());
            ps.setBigDecimal(3, building.getShippingFee());
            ps.setInt(4, building.getBuildingId());
            ps.executeUpdate();
        }
    }

    @Override
    public void setActive(Connection conn, int buildingId, boolean active) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SET_ACTIVE)) {
            ps.setBoolean(1, active);
            ps.setInt(2, buildingId);
            ps.executeUpdate();
        }
    }

    private Building mapRow(ResultSet rs) throws SQLException {
        Building building = new Building();
        building.setBuildingId(rs.getInt("building_id"));
        building.setName(rs.getString("name"));
        building.setDescription(rs.getString("description"));
        building.setShippingFee(rs.getBigDecimal("shipping_fee"));
        building.setActive(rs.getBoolean("is_active"));
        return building;
    }
}
