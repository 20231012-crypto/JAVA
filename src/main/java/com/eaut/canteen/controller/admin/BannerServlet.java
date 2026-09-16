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
import com.eaut.canteen.util.AppClock;
import com.eaut.canteen.util.DBConnection;
import com.eaut.canteen.util.ImageResizer;
import com.eaut.canteen.util.RequestParams;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

/** Admin CRUD for banners.manage — list/create/edit/delete/toggle, per position (HEAD/FOOTER/LEFT/RIGHT). */
@WebServlet({"/admin/banners", "/admin/banners/form", "/admin/banners/save", "/admin/banners/toggle", "/admin/banners/delete"})
// 20MB so a short promo clip fits. Deliberately not larger: the media lives in a database column
// and BannerImageServlet reads the whole row into memory to serve it, so this is a size the app
// can hold per request without trouble — it is a banner slot, not a video host.
@MultipartConfig(maxFileSize = 20L * 1024 * 1024)
public class BannerServlet extends HttpServlet {

    private static final Set<String> VALID_POSITIONS = Set.of("HEAD", "FOOTER", "LEFT", "RIGHT");
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    // MP4 (H.264) and WebM cover every current browser between them.
    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of("video/mp4", "video/webm");

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
        Integer id = RequestParams.intOrNull(req.getParameter("id"));
        Banner banner = id == null ? null : bannerDAO.findById(conn, id);
        req.setAttribute("pageTitle", banner == null ? "Thêm banner" : "Sửa banner");
        req.setAttribute("banner", banner);
        req.getRequestDispatcher("/WEB-INF/views/admin/banner-form.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try (Connection conn = DBConnection.getConnection()) {
            switch (req.getServletPath()) {
                case "/admin/banners/toggle" -> {
                    Integer toggleId = RequestParams.intOrNull(req.getParameter("bannerId"));
                    if (toggleId == null) {
                        resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    }
                    bannerDAO.setActive(conn, toggleId, Boolean.parseBoolean(req.getParameter("active")));
                    resp.sendRedirect(req.getContextPath() + "/admin/banners");
                }
                case "/admin/banners/delete" -> {
                    Integer deleteId = RequestParams.intOrNull(req.getParameter("bannerId"));
                    if (deleteId == null) {
                        resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    }
                    bannerDAO.delete(conn, deleteId);
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
        banner.setSortOrder(RequestParams.intOrDefault(req.getParameter("sortOrder"), 0));
        // The admin types these in canteen time; the columns store server time. Without the
        // conversion a banner scheduled for 07:00 would appear at 14:00 in Hanoi, because on
        // Render the JVM runs UTC.
        banner.setStartAt(parseWindowBound(conn, req.getParameter("startAt")));
        banner.setEndAt(parseWindowBound(conn, req.getParameter("endAt")));

        String idParam = req.getParameter("bannerId");
        boolean isNew = idParam == null || idParam.isBlank();

        // Validate the upload BEFORE writing anything: this used to insert the row first, so a
        // rejected file left behind an empty banner that was already live on the catalog page.
        Part imagePart = req.getPart("image");
        boolean hasUpload = imagePart != null && imagePart.getSize() > 0;
        String contentType = hasUpload ? imagePart.getContentType() : null;
        if (hasUpload && (contentType == null
                || !(ALLOWED_IMAGE_TYPES.contains(contentType) || ALLOWED_VIDEO_TYPES.contains(contentType)))) {
            req.setAttribute("pageTitle", isNew ? "Thêm banner" : "Sửa banner");
            req.setAttribute("banner", banner);
            req.setAttribute("error", "Định dạng không được hỗ trợ. Ảnh: JPG, PNG, WEBP. Video: MP4, WEBM.");
            req.getRequestDispatcher("/WEB-INF/views/admin/banner-form.jsp").forward(req, resp);
            return;
        }

        if (isNew) {
            bannerDAO.insert(conn, banner);
        } else {
            Integer bannerId = RequestParams.intOrNull(idParam);
            if (bannerId == null) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            banner.setBannerId(bannerId);
            bannerDAO.update(conn, banner);
        }

        if (hasUpload) {
            byte[] imageData = readAllBytes(imagePart.getInputStream());
            // Images only. A banner may also be a short video clip, and feeding those bytes to an
            // image decoder would either fail or — worse — silently replace the clip with a still.
            if (ALLOWED_IMAGE_TYPES.contains(contentType)) {
                imageData = ImageResizer.shrink(imageData);
            }
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

    /**
     * Reads one end of the scheduling window from a datetime-local field. Blank means "no limit",
     * and an unparseable value is treated the same way rather than throwing: the field is optional
     * and a malformed one should leave the banner unscheduled, not 500 the save.
     */
    private java.time.LocalDateTime parseWindowBound(Connection conn, String raw) throws SQLException {
        String trimmed = RequestParams.trimmedOrNull(raw);
        if (trimmed == null) {
            return null;
        }
        try {
            return AppClock.fromCanteenInput(conn, java.time.LocalDateTime.parse(trimmed));
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }
}
