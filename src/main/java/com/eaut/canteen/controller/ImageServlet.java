package com.eaut.canteen.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.eaut.canteen.util.AppConfig;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Streams product images from either of two sources:
 * <ul>
 *   <li>the external upload directory (see FileUploadUtil) — admin-uploaded images, which live
 *       outside the WAR and are lost on Render since every deploy is a brand-new container;</li>
 *   <li>{@code /WEB-INF/seed-images/} — the bundled catalog photos shipped with the app itself,
 *       checked into git and packaged into the WAR, so they survive every redeploy. Checked
 *       second so an admin re-uploading a replacement for a seeded product still wins.</li>
 * </ul>
 */
@WebServlet("/images/*")
public class ImageServlet extends HttpServlet {

    private static final String SEED_IMAGES_PATH = "/WEB-INF/seed-images/";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() < 2) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String filename = pathInfo.substring(1);
        Path uploadDir = Path.of(AppConfig.get("upload.dir")).normalize();
        Path target = uploadDir.resolve(filename).normalize();

        // Defense in depth against path traversal, even though filenames are server-generated UUIDs.
        if (target.startsWith(uploadDir) && Files.isRegularFile(target)) {
            String contentType = Files.probeContentType(target);
            resp.setContentType(contentType != null ? contentType : "application/octet-stream");
            Files.copy(target, resp.getOutputStream());
            return;
        }

        // Filename comes from the DB (products.image_filename), never from the request path
        // directly, and WEB-INF resources cannot escape their own directory via getResource.
        try (InputStream seedStream = getServletContext().getResourceAsStream(SEED_IMAGES_PATH + filename)) {
            if (seedStream == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            String contentType = getServletContext().getMimeType(filename);
            resp.setContentType(contentType != null ? contentType : "application/octet-stream");
            seedStream.transferTo(resp.getOutputStream());
        }
    }
}
