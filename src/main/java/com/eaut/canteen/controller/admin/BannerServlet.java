package com.eaut.canteen.controller.admin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import com.eaut.canteen.dao.BannerDAO;
import com.eaut.canteen.dao.impl.BannerDAOImpl;
import com.eaut.canteen.model.Banner;
import com.eaut.canteen.util.DBConnection;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

/** Admin CRUD for banners.manage — list/create/edit/delete/toggle, per position (HEAD/FOOTER/LEFT/RIGHT). */
@WebServlet({"/admin/banners", "/admin/banners/form", "/admin/banners/save", "/admin/banners/toggle", "/admin/banners/delete"})
@MultipartConfig(maxFileSize = 5 * 1024 * 1024)
public class BannerServlet extends HttpServlet {

    private static final Set<String> VALID_POSITIONS = Set.of("HEAD", "FOOTER", "LEFT", "RIGHT");
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final BannerDAO bannerDAO = new BannerDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            if ("/admin/banners/form".equals(req.getServletPath())) {
                showForm(req, resp, conn);
            } else {
                req.setAttribute("pageTitle", "Banner trang chủ");
                req.setAttribute("banners", bannerDAO.findAllForAdmin(conn));
                req.getRequestDispatcher("/WEB-INF/views/admin/banners.jsp").forward(req, resp);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void showForm(HttpServletRequest req, HttpServletResponse resp, Connection conn)
            throws SQLException, ServletException, IOException {
        String idParam = req.getParameter("id");
        Banner banner = null;
        if (idParam != null && !idParam.isBlank()) {
            banner = bannerDAO.findById(conn, Integer.parseInt(idParam));
        }
        req.setAttribute("pageTitle", banner == null ? "Thêm banner" : "Sửa banner");
        req.setAttribute("banner", banner);
        req.getRequestDispatcher("/WEB-INF/views/admin/banner-form.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            switch (req.getServletPath()) {
                case "/admin/banners/toggle" -> {
                    bannerDAO.setActive(conn, Integer.parseInt(req.getParameter("bannerId")),
                            Boolean.parseBoolean(req.getParameter("active")));
                    resp.sendRedirect(req.getContextPath() + "/admin/banners");
                }
                case "/admin/banners/delete" -> {
                    bannerDAO.delete(conn, Integer.parseInt(req.getParameter("bannerId")));
                    resp.sendRedirect(req.getContextPath() + "/admin/banners");
                }
                default -> saveBanner(req, resp, conn);
            }
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void saveBanner(HttpServletRequest req, HttpServletResponse resp, Connection conn)
            throws SQLException, ServletException, IOException {
        String position = req.getParameter("position");
        if (position == null || !VALID_POSITIONS.contains(position)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        Banner banner = new Banner();
        banner.setPosition(position);
        banner.setTitle(blankToNull(req.getParameter("title")));
        banner.setSubtitle(blankToNull(req.getParameter("subtitle")));
        banner.setLinkUrl(blankToNull(req.getParameter("linkUrl")));
        String sortOrder = req.getParameter("sortOrder");
        banner.setSortOrder(sortOrder == null || sortOrder.isBlank() ? 0 : Integer.parseInt(sortOrder));

        String idParam = req.getParameter("bannerId");
        boolean isNew = idParam == null || idParam.isBlank();
        if (isNew) {
            bannerDAO.insert(conn, banner);
        } else {
            banner.setBannerId(Integer.parseInt(idParam));
            bannerDAO.update(conn, banner);
        }

        Part imagePart = req.getPart("image");
        if (imagePart != null && imagePart.getSize() > 0) {
            String contentType = imagePart.getContentType();
            if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
                req.setAttribute("pageTitle", isNew ? "Thêm banner" : "Sửa banner");
                req.setAttribute("banner", banner);
                req.setAttribute("error", "Định dạng ảnh không được hỗ trợ. Chỉ chấp nhận JPG, PNG hoặc WEBP.");
                req.getRequestDispatcher("/WEB-INF/views/admin/banner-form.jsp").forward(req, resp);
                return;
            }
            byte[] imageData = readAllBytes(imagePart.getInputStream());
            bannerDAO.updateImage(conn, banner.getBannerId(), imageData, contentType);
        }

        resp.sendRedirect(req.getContextPath() + "/admin/banners");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        in.transferTo(out);
        return out.toByteArray();
    }
}
