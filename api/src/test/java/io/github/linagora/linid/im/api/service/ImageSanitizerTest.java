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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.linagora.linid.im.corelib.exception.ApiException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Test class: ImageSanitizer")
class ImageSanitizerTest {

    private final ImageSanitizer sanitizer = new ImageSanitizer("png", 0.9f);

    private static byte[] pngBytes(final int width, final int height, final int color) throws IOException {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, color);
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return bytes.toByteArray();
    }

    @Test
    @DisplayName("Should re-encode a valid image keeping its dimensions and pixels")
    void testSanitize() throws IOException {
        var sanitized = sanitizer.sanitize(pngBytes(2, 3, 0xFF0000));

        var image = ImageIO.read(new ByteArrayInputStream(sanitized));
        assertEquals(2, image.getWidth());
        assertEquals(3, image.getHeight());
        assertEquals(0xFF0000, image.getRGB(0, 0) & 0xFFFFFF);
    }

    @Test
    @DisplayName("Should drop a payload appended after the image data")
    void testSanitizeDropsTrailingPayload() throws IOException {
        var payload = "<script>alert(1)</script>";
        var bytes = new ByteArrayOutputStream();
        bytes.write(pngBytes(1, 1, 0xFF0000));
        bytes.write(payload.getBytes(StandardCharsets.UTF_8));

        var sanitized = sanitizer.sanitize(bytes.toByteArray());

        assertFalse(new String(sanitized, StandardCharsets.ISO_8859_1).contains(payload));
    }

    @Test
    @DisplayName("Should return 400 when the content is not an image")
    void testSanitizeNotAnImage() {
        var exception = assertThrows(ApiException.class,
            () -> sanitizer.sanitize("not an image".getBytes(StandardCharsets.UTF_8)));

        assertEquals(400, exception.getStatusCode());
        assertEquals("error.avatar.content.invalid", exception.getError().key());
    }

    @Test
    @DisplayName("Should return 400 when the content is an image of another type")
    void testSanitizeWrongImageType() throws IOException {
        var image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", bytes);

        var exception = assertThrows(ApiException.class, () -> sanitizer.sanitize(bytes.toByteArray()));

        assertEquals(400, exception.getStatusCode());
        assertEquals("error.avatar.content.invalid", exception.getError().key());
    }

    @Test
    @DisplayName("Should return 400 when the image dimensions exceed the allowed maximum")
    void testSanitizeOversizedDimensions() throws IOException {
        var png = pngBytes(1, 1, 0xFF0000);
        // The IHDR chunk holds the big-endian width at offset 16: announce an image too large to decode.
        png[16] = (byte) 0x7F;
        png[17] = (byte) 0xFF;
        png[18] = (byte) 0xFF;
        png[19] = (byte) 0xFF;

        var exception = assertThrows(ApiException.class, () -> sanitizer.sanitize(png));

        assertEquals(400, exception.getStatusCode());
    }

    @Test
    @DisplayName("Should reject an unsupported configured extension at construction")
    void testUnsupportedExtension() {
        assertThrows(IllegalStateException.class, () -> new ImageSanitizer("svg", 0.9f));
    }
}
