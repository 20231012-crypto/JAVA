package com.eaut.canteen.dao.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.eaut.canteen.dao.AppSettingDAO;
import com.eaut.canteen.model.AppSetting;

public class AppSettingDAOImpl implements AppSettingDAO {

    // sort_order is the primary key of the layout: it groups related settings together and puts
    // the ones a manager changes most at the top of their section.
    private static final String FIND_ALL =
            "SELECT s.*, u.full_name AS updated_by_name " +
            "FROM app_settings s LEFT JOIN users u ON s.updated_by = u.user_id " +
            "ORDER BY s.sort_order, s.setting_key";

    @Override
    public List<AppSetting> findAll(Connection conn) throws SQLException {
        List<AppSetting> settings = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(FIND_ALL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                AppSetting setting = new AppSetting();
                setting.setSettingKey(rs.getString("setting_key"));
                setting.setSettingValue(rs.getString("setting_value"));
                setting.setValueType(rs.getString("value_type"));
                setting.setGroupName(rs.getString("group_name"));
                setting.setDisplayName(rs.getString("display_name"));
                setting.setHint(rs.getString("hint"));
                setting.setSortOrder(rs.getInt("sort_order"));
                if (rs.getTimestamp("updated_at") != null) {
                    setting.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                }
                setting.setUpdatedByName(rs.getString("updated_by_name"));
                settings.add(setting);
            }
        }
        return settings;
    }
}
