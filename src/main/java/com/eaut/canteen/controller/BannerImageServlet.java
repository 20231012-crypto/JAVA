package com.eaut.canteen.controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.eaut.canteen.dao.BannerDAO;
import com.eaut.canteen.dao.impl.BannerDAOImpl;
import com.eaut.canteen.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Streams a banner's image straight from the database (banners.image_data) rather than a file
 * on disk — see the migration comment for why: the upload directory is Render container-local
 * and does not survive a redeploy, but a database row does.
 */
@WebServlet("/banner-image")
public class BannerImageServlet extends HttpServlet {

    private final BannerDAO bannerDAO = new BannerDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int bannerId;
        try {
            bannerId = Integer.parseInt(req.getParameter("id"));
        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            byte[] imageData = bannerDAO.findImageData(conn, bannerId);
            if (imageData == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            String contentType = bannerDAO.findImageContentType(conn, bannerId);
            resp.setContentType(contentType != null ? contentType : "application/octet-stream");
            resp.setContentLength(imageData.length);
            resp.getOutputStream().write(imageData);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }
}
