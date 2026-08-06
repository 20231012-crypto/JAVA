package com.eaut.canteen.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.eaut.canteen.dao.BuildingDAO;
import com.eaut.canteen.model.Building;

public class BuildingDAOImpl implements BuildingDAO {

    private static final String FIND_ALL_ACTIVE =
            "SELECT * FROM buildings WHERE is_active = TRUE ORDER BY name";
    private static final String FIND_BY_ID =
            "SELECT * FROM buildings WHERE building_id = ?";

    @Override
    public List<Building> findAllActive(Connection conn) throws SQLException {
        List<Building> buildings = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_ALL_ACTIVE);
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
