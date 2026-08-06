package com.eaut.canteen.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.http.Part;

public final class FileUploadUtil {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private FileUploadUtil() {
    }

    /**
     * Validates and saves an uploaded product image to the configured upload directory,
     * using a generated filename (never the user-supplied one, to close path-traversal /
     * overwrite / "upload a .jsp" attacks).
     *
     * @return the generated filename to store on the product row, or null if no file was submitted.
     */
    public static String saveProductImage(Part filePart) throws IOException {
        if (filePart == null || filePart.getSize() == 0) {
            return null;
        }

        String submittedName = filePart.getSubmittedFileName();
        String extension = extractExtension(submittedName);
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IOException("Định dạng ảnh không được hỗ trợ. Chỉ chấp nhận JPG, PNG, WEBP.");
        }

        String generatedName = UUID.randomUUID() + "." + extension;
        Path uploadDir = Path.of(AppConfig.get("upload.dir"));
        Files.createDirectories(uploadDir);
        Path target = uploadDir.resolve(generatedName);

        try (InputStream in = filePart.getInputStream()) {
            Files.copy(in, target);
        }

        return generatedName;
    }

    private static String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return null;
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
