package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.model.Category;

public interface CategoryDAO {

    List<Category> findAllActive(Connection conn) throws SQLException;
}
