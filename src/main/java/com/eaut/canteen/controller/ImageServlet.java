package com.eaut.canteen.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.eaut.canteen.util.AppConfig;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Streams uploaded product images from the external upload directory (see FileUploadUtil). */
@WebServlet("/images/*")
public class ImageServlet extends HttpServlet {

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
        if (!target.startsWith(uploadDir) || !Files.isRegularFile(target)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String contentType = Files.probeContentType(target);
        resp.setContentType(contentType != null ? contentType : "application/octet-stream");
        Files.copy(target, resp.getOutputStream());
    }
}
