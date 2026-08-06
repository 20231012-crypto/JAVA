package com.eaut.canteen.controller.admin;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

import com.eaut.canteen.dao.BuildingDAO;
import com.eaut.canteen.dao.impl.BuildingDAOImpl;
import com.eaut.canteen.model.Building;
import com.eaut.canteen.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet({"/admin/buildings", "/admin/buildings/save", "/admin/buildings/toggle"})
public class BuildingServlet extends HttpServlet {

    private static final BuildingDAO buildingDAO = new BuildingDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            req.setAttribute("pageTitle", "Tòa nhà & phí ship");
            req.setAttribute("buildings", buildingDAO.findAll(conn));
            req.getRequestDispatcher("/WEB-INF/views/admin/buildings.jsp").forward(req, resp);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            if ("/admin/buildings/toggle".equals(req.getServletPath())) {
                int buildingId = Integer.parseInt(req.getParameter("buildingId"));
                boolean active = Boolean.parseBoolean(req.getParameter("active"));
                buildingDAO.setActive(conn, buildingId, active);
            } else {
                saveBuilding(req, conn);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
        resp.sendRedirect(req.getContextPath() + "/admin/buildings");
    }

    private void saveBuilding(HttpServletRequest req, Connection conn) throws SQLException {
        String name = req.getParameter("name");
        if (name == null || name.isBlank()) {
            return;
        }
        Building building = new Building();
        building.setName(name.trim());
        building.setDescription(req.getParameter("description"));
        building.setShippingFee(new BigDecimal(req.getParameter("shippingFee")));

        String idParam = req.getParameter("buildingId");
        if (idParam == null || idParam.isBlank()) {
            buildingDAO.insert(conn, building);
        } else {
            building.setBuildingId(Integer.parseInt(idParam));
            buildingDAO.update(conn, building);
        }
    }
}
