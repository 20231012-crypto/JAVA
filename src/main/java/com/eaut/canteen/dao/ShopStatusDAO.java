package com.eaut.canteen.dao;

import java.sql.Connection;
import java.sql.SQLException;

import com.eaut.canteen.model.ShopStatus;

/** The single-row shop_status switch — "Mở đơn / Tạm ngưng nhận đơn" (see ShopStatusServlet, CheckoutServlet). */
public interface ShopStatusDAO {

    ShopStatus get(Connection conn) throws SQLException;

    void setAcceptingOrders(Connection conn, boolean acceptingOrders, int updatedBy) throws SQLException;
}
