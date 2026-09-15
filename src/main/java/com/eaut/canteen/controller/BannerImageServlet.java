package com.eaut.canteen.controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import com.eaut.canteen.dao.BannerDAO;
import com.eaut.canteen.dao.impl.BannerDAOImpl;
import com.eaut.canteen.util.DBConnection;
import com.eaut.canteen.util.RequestParams;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Streams a banner's media (image or short video) straight from the database
 * (banners.image_data) rather than a file on disk — see the migration comment for why: the
 * upload directory is Render container-local and does not survive a redeploy, but a database
 * row does.
 *
 * Serves byte ranges as well as whole files. That is not an optimisation: Safari (desktop and
 * iOS) will not play a &lt;video&gt; at all from an endpoint that ignores Range, so without this
 * a video banner would simply show nothing on every iPhone.
 */
@WebServlet("/banner-image")
public class BannerImageServlet extends HttpServlet {

    private final BannerDAO bannerDAO = new BannerDAOImpl();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Integer bannerId = RequestParams.intOrNull(req.getParameter("id"));
        if (bannerId == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            byte[] data = bannerDAO.findImageData(conn, bannerId);
            if (data == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            String contentType = bannerDAO.findImageContentType(conn, bannerId);
            // EncodingFilter sets UTF-8 on every response, which the container then merges into the
            // content type as "video/mp4;charset=UTF-8". Binary media has no charset and stricter
            // players reject the parameter, so clear the encoding for this response only.
            resp.setCharacterEncoding(null);
            resp.setContentType(contentType != null ? contentType : "application/octet-stream");
            resp.setHeader("Accept-Ranges", "bytes");

            long[] range = parseRange(req.getHeader("Range"), data.length);
            if (range == null) {
                resp.setContentLength(data.length);
                resp.getOutputStream().write(data);
                return;
            }

            int start = (int) range[0];
            int end = (int) range[1];
            int length = end - start + 1;
            resp.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
            resp.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + data.length);
            resp.setContentLength(length);
            resp.getOutputStream().write(data, start, length);
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    /**
     * Parses a single "bytes=start-end" range, the only form a media element actually sends.
     * Returns null when the header is absent, malformed, a multi-range request, or out of
     * bounds — every one of which is answered correctly by just sending the whole thing.
     */
    private long[] parseRange(String header, int totalLength) {
        if (header == null || !header.startsWith("bytes=") || header.contains(",")) {
            return null;
        }
        String spec = header.substring("bytes=".length()).trim();
        int dash = spec.indexOf('-');
        if (dash < 0) {
            return null;
        }
        try {
            String startText = spec.substring(0, dash).trim();
            String endText = spec.substring(dash + 1).trim();
            long start;
            long end;
            if (startText.isEmpty()) {
                // "bytes=-500" means the last 500 bytes.
                long suffixLength = Long.parseLong(endText);
                if (suffixLength <= 0) {
                    return null;
                }
                start = Math.max(0, totalLength - suffixLength);
                end = totalLength - 1L;
            } else {
                start = Long.parseLong(startText);
                end = endText.isEmpty() ? totalLength - 1L : Long.parseLong(endText);
            }
            if (start < 0 || end < start || start >= totalLength) {
                return null;
            }
            return new long[] {start, Math.min(end, totalLength - 1L)};
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
