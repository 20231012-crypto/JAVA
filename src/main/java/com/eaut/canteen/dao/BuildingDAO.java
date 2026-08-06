package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.model.Building;

public interface BuildingDAO {

    List<Building> findAllActive(Connection conn) throws SQLException;

    Building findById(Connection conn, int buildingId) throws SQLException;
}
