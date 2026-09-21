/*
 * Copyright (C) 2020-2026 Linagora
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version, provided you comply with the Additional Terms applicable for LinID Identity Manager software by
 * LINAGORA pursuant to Section 7 of the GNU Affero General Public License, subsections (b), (c), and (e), pursuant to
 * which these Appropriate Legal Notices must notably (i) retain the display of the "LinID™" trademark/logo at the top
 * of the interface window, the display of the “You are using the Open Source and free version of LinID™, powered by
 * Linagora © 2009–2013. Contribute to LinID R&D by subscribing to an Enterprise offer!” infobox and in the e-mails
 * sent with the Program, notice appended to any type of outbound messages (e.g. e-mail and meeting requests) as well
 * as in the LinID Identity Manager user interface, (ii) retain all hypertext links between LinID Identity Manager
 * and https://linid.org/, as well as between LINAGORA and LINAGORA.com, and (iii) refrain from infringing LINAGORA
 * intellectual property rights over its trademarks and commercial brands. Other Additional Terms apply, see
 * <http://www.linagora.com/licenses/> for more details.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License and its applicable Additional Terms for
 * LinID Identity Manager along with this program. If not, see <http://www.gnu.org/licenses/> for the GNU Affero
 * General Public License version 3 and <http://www.linagora.com/licenses/> for the Additional Terms applicable to the
 * LinID Identity Manager software.
 */


package io.github.linagora.linid.im.api.service;

import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Validates an uploaded avatar image and rebuilds it from its pixels only.
 *
 * <ol>
 *   <li>Real type detection on the content (magic bytes), checked against the configured extension: the file
 *       name and the declared content type are ignored on purpose.</li>
 *   <li>Decoding with the reader matching the detected type, after a dimension check bounding the pixel
 *       buffer (decompression bomb guard).</li>
 *   <li>Redraw into a fresh buffer and re-encoding: trailing payloads, metadata and anything that is not
 *       pixel data are dropped.</li>
 * </ol>
 */
@Component
public class ImageSanitizer {

    /**
     * MIME type of each supported avatar file extension.
     */
    private static final Map<String, String> MIME_TYPES_BY_EXTENSION = Map.of(
        "png", "image/png",
        "jpg", "image/jpeg",
        "jpeg", "image/jpeg");

    /**
     * Maximum width times height, checked before decoding.
     */
    private static final long MAX_PIXELS = 16_000_000L;

    /**
     * Content type detector.
     */
    private final Tika tika = new Tika();

    /**
     * Only allowed avatar file extension.
     */
    private final String extension;

    /**
     * MIME type matching {@link #extension}.
     */
    private final String mimeType;

    /**
     * Compression quality of the re-encoded JPEG images.
     */
    private final float jpegQuality;

    /**
     * Creates the sanitizer.
     *
     * @param extension   only allowed avatar file extension
     * @param jpegQuality compression quality of the re-encoded JPEG images
     */
    public ImageSanitizer(
        @Value("${avatar.extension}") final String extension,
        @Value("${avatar.jpeg-quality}") final float jpegQuality) {
        this.extension = extension.toLowerCase(Locale.ROOT);
        this.mimeType = MIME_TYPES_BY_EXTENSION.get(this.extension);
        this.jpegQuality = jpegQuality;

        if (mimeType == null) {
            throw new IllegalStateException("No image codec mapping for the avatar extension " + extension);
        }
    }

    /**
     * Validates the content of an uploaded image and re-encodes it from its pixels only.
     *
     * @param raw the raw uploaded content
     * @return the re-encoded image, safe to store and serve
     * @throws ApiException with HTTP 400 status when the content is not an image of the configured type
     */
    public byte[] sanitize(final byte[] raw) {
        if (!mimeType.equals(tika.detect(raw))) {
            throw contentException();
        }

        BufferedImage decoded = decode(raw);
        BufferedImage pixelsOnly = redraw(decoded);

        return encode(pixelsOnly);
    }

    /**
     * Decodes the image with the reader of the expected type only, after bounding its dimensions.
     *
     * @param raw the raw uploaded content
     * @return the decoded image
     */
    private BufferedImage decode(final byte[] raw) {
        ImageReader reader = first(ImageIO.getImageReadersByMIMEType(mimeType));

        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(raw))) {
            reader.setInput(input, true, true);

            long pixels = (long) reader.getWidth(0) * reader.getHeight(0);

            if (pixels <= 0 || pixels > MAX_PIXELS) {
                throw new ApiException(HttpStatus.BAD_REQUEST.value(),
                    I18nMessage.of("error.avatar.dimensions.invalid"));
            }

            return reader.read(0);
        } catch (ApiException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            // ImageIO readers throw assorted runtime exceptions on malformed input.
            throw contentException();
        } finally {
            reader.dispose();
        }
    }

    /**
     * Copies the pixels into a plain RGB or ARGB buffer, dropping exotic color models.
     *
     * @param source the decoded image
     * @return the redrawn image
     */
    private BufferedImage redraw(final BufferedImage source) {
        int type = BufferedImage.TYPE_INT_RGB;

        if ("image/png".equals(mimeType) && source.getColorModel().hasAlpha()) {
            type = BufferedImage.TYPE_INT_ARGB;
        }

        var target = new BufferedImage(source.getWidth(), source.getHeight(), type);
        Graphics2D graphics = target.createGraphics();

        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }

        return target;
    }

    /**
     * Re-encodes the image with default metadata only: no EXIF, comments or ancillary chunks.
     *
     * @param image the redrawn image
     * @return the encoded image bytes
     */
    private byte[] encode(final BufferedImage image) {
        ImageWriter writer = first(ImageIO.getImageWritersByMIMEType(mimeType));
        var bytes = new ByteArrayOutputStream();

        try (var output = new MemoryCacheImageOutputStream(bytes)) {
            writer.setOutput(output);

            ImageWriteParam param = writer.getDefaultWriteParam();

            if ("image/jpeg".equals(mimeType)) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(jpegQuality);
            }

            writer.write(null, new IIOImage(image, null, null), param);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to re-encode the avatar image", e);
        } finally {
            writer.dispose();
        }

        return bytes.toByteArray();
    }

    /**
     * Returns the rejection of a content that is not an image of the configured type.
     *
     * @return the exception to throw
     */
    private ApiException contentException() {
        return new ApiException(HttpStatus.BAD_REQUEST.value(),
            I18nMessage.of("error.avatar.content.invalid", Map.of("extension", extension)));
    }

    /**
     * Returns the first available ImageIO codec.
     *
     * @param codecs the codecs matching the expected type
     * @param <T>    the codec type
     * @return the first codec
     */
    private <T> T first(final Iterator<T> codecs) {
        if (!codecs.hasNext()) {
            throw new IllegalStateException("No ImageIO codec available for " + mimeType);
        }

        return codecs.next();
    }
}
