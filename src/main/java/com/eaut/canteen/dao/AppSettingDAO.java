package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import com.eaut.canteen.model.AppSetting;

/**
 * Reads the settings catalogue for the admin screen. Writing a value goes through
 * {@link com.eaut.canteen.util.Settings#set} instead, because that also drops the read cache —
 * bypassing it would leave the site serving the old value for up to the cache TTL.
 */
public interface AppSettingDAO {

    /** Every setting, ordered by group then sort_order, which is the order the screen renders. */
    List<AppSetting> findAll(Connection conn) throws SQLException;
}
