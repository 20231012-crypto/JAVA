package com.eaut.canteen.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * Shrinks an uploaded photo to something a web page can actually use.
 *
 * <p>Why this exists: the seed dish photos shipped at 1264x848 and roughly 800KB each, while a
 * product card renders them about 200 CSS px wide. Twenty-six of those made the menu page pull
 * around 20MB — which on a phone in Vietnam dwarfed every millisecond of server time by an order
 * of magnitude. Re-encoding them at 640px and quality 0.82 cut the set from 11.2MB to 1.2MB with
 * no visible difference at the size they are displayed.
 *
 * <p>Resizing on upload rather than only fixing the existing files is the point: otherwise the next
 * photo a manager uploads straight from a phone camera puts the problem back, and nobody would
 * notice until the page was slow again.
 *
 * <p>Uses only ImageIO from the JDK. An image library would do a slightly better job of downscaling
 * but is not worth a dependency for one operation on a handful of photos a term.
 */
public final class ImageResizer {

    /** Wide enough for a card on a 2x-density phone; well past what any layout here shows. */
    private static final int MAX_WIDTH = 640;

    /** Where JPEG stops paying for detail that is invisible at this size. */
    private static final float QUALITY = 0.82f;

    private ImageResizer() {
    }

    /**
     * Returns {@code original} re-encoded smaller, or {@code original} unchanged when it is already
     * small enough, is not an image this JVM can decode, or when re-encoding would somehow produce
     * a larger file.
     *
     * <p>Never throws on unreadable input: this runs in the middle of a save, and an upload that is
     * merely unusual should be stored as-is rather than failing the whole form. The caller has
     * already checked the file type.
     */
    public static byte[] shrink(byte[] original) {
        if (original == null || original.length == 0) {
            return original;
        }
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(original));
            if (source == null || source.getWidth() <= MAX_WIDTH) {
                return original;
            }

            int width = MAX_WIDTH;
            int height = Math.round(source.getHeight() * (width / (float) source.getWidth()));

            // TYPE_INT_RGB rather than ARGB: the output is JPEG, which has no alpha channel, and
            // writing one produces a file some decoders render with inverted colours.
            BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.drawImage(source, 0, 0, width, height, null);
            g.dispose();

            byte[] shrunk = encodeJpeg(scaled);
            // A photo that was already well compressed can come out bigger; keep whichever is
            // smaller so this can never make things worse.
            return shrunk != null && shrunk.length < original.length ? shrunk : original;
        } catch (IOException | RuntimeException e) {
            return original;
        }
    }

    private static byte[] encodeJpeg(BufferedImage image) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            return null;
        }
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(QUALITY);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ImageOutputStream out = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(out);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }
}
