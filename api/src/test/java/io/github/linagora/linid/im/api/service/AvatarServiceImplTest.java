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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.linagora.linid.im.api.model.common.AvatarEntity;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

@DisplayName("Test class: AvatarServiceImpl")
class AvatarServiceImplTest {

    @TempDir
    private Path location;

    private AvatarServiceImpl service;

    private UUID id;

    @BeforeEach
    void setUp() {
        service = new AvatarServiceImpl(location.toString(), "png", new ImageSanitizer("png", 0.9f));
        id = UUID.randomUUID();
    }

    private MockMultipartFile file(final String name, final byte[] content) {
        return new MockMultipartFile("file", name, "image/png", content);
    }

    private static byte[] pngBytes(final int color) throws IOException {
        var image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, color);
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return bytes.toByteArray();
    }

    private int storedColor(final String entity) throws IOException {
        var image = ImageIO.read(avatarPath(entity).toFile());
        return image.getRGB(0, 0) & 0xFFFFFF;
    }

    private Path avatarPath(final String entity) {
        return location.resolve(entity).resolve(id + ".png");
    }

    @Test
    @DisplayName("Should store the avatar of an existing account")
    void testUploadAccount() throws IOException {
        service.upload(AvatarEntity.ACCOUNTS, id, file("me.png", pngBytes(0xFF0000)));

        assertEquals(0xFF0000, storedColor("accounts"));
    }

    @Test
    @DisplayName("Should store the avatar with world-readable permissions")
    void testUploadMakesAvatarWorldReadable() throws IOException {
        service.upload(AvatarEntity.ACCOUNTS, id, file("me.png", pngBytes(0xFF0000)));

        var permissions = Files.getPosixFilePermissions(avatarPath("accounts"));

        assertTrue(permissions.contains(PosixFilePermission.OTHERS_READ));
    }

    @Test
    @DisplayName("Should store the avatar of an existing application")
    void testUploadApplication() throws IOException {
        service.upload(AvatarEntity.APPLICATIONS, id, file("app.PNG", pngBytes(0x00FF00)));

        assertTrue(Files.exists(avatarPath("applications")));
    }

    @Test
    @DisplayName("Should store the avatar of an existing group")
    void testUploadGroup() throws IOException {
        service.upload(AvatarEntity.GROUPS, id, file("group.png", pngBytes(0x00FF00)));

        assertTrue(Files.exists(avatarPath("groups")));
    }

    @Test
    @DisplayName("Should store the avatar of an existing organizational unit")
    void testUploadOrganizationalUnit() throws IOException {
        service.upload(AvatarEntity.ORGANIZATIONAL_UNITS, id, file("ou.png", pngBytes(0x00FF00)));

        assertTrue(Files.exists(avatarPath("organizational-units")));
    }

    @Test
    @DisplayName("Should replace an existing avatar")
    void testUploadReplacesExistingAvatar() throws IOException {
        service.upload(AvatarEntity.ACCOUNTS, id, file("old.png", pngBytes(0xFF0000)));

        service.upload(AvatarEntity.ACCOUNTS, id, file("new.png", pngBytes(0x0000FF)));

        assertEquals(0x0000FF, storedColor("accounts"));
    }

    @Test
    @DisplayName("Should return 400 when the file is empty")
    void testUploadEmptyFile() {
        var exception = assertThrows(ApiException.class,
            () -> service.upload(AvatarEntity.ACCOUNTS, id, file("me.png", new byte[0])));

        assertEquals(400, exception.getStatusCode());
        assertEquals("error.avatar.file.empty", exception.getError().key());
    }

    @Test
    @DisplayName("Should return 400 when the file extension is not the configured one")
    void testUploadInvalidExtension() {
        var exception = assertThrows(ApiException.class,
            () -> service.upload(AvatarEntity.ACCOUNTS, id, file("me.jpg", pngBytes(0xFF0000))));

        assertEquals(400, exception.getStatusCode());
        assertEquals("error.avatar.extension.invalid", exception.getError().key());
        assertEquals(Map.of("extension", "png"), exception.getError().context());
        assertFalse(Files.exists(avatarPath("accounts")));
    }

    @Test
    @DisplayName("Should return 400 when the file has no extension")
    void testUploadMissingExtension() {
        var exception = assertThrows(ApiException.class,
            () -> service.upload(AvatarEntity.ACCOUNTS, id, file("me", pngBytes(0xFF0000))));

        assertEquals("error.avatar.extension.invalid", exception.getError().key());
    }


    @Test
    @DisplayName("Should return 500 when the file cannot be stored")
    void testUploadStorageFailure() throws IOException {
        Files.createFile(location.resolve("accounts"));

        var exception = assertThrows(ApiException.class,
            () -> service.upload(AvatarEntity.ACCOUNTS, id, file("me.png", pngBytes(0xFF0000))));

        assertEquals(500, exception.getStatusCode());
        assertEquals("error.avatar.storage", exception.getError().key());
    }

    @Test
    @DisplayName("Should keep the previous avatar when the upload fails")
    void testUploadFailureKeepsPreviousAvatar() throws IOException {
        service.upload(AvatarEntity.ACCOUNTS, id, file("old.png", pngBytes(0xFF0000)));
        var failingFile = new MockMultipartFile("file", "new.png", "image/png", pngBytes(0x0000FF)) {
            @Override
            public byte[] getBytes() throws IOException {
                throw new IOException("Connection reset during the upload");
            }
        };

        var exception = assertThrows(ApiException.class,
            () -> service.upload(AvatarEntity.ACCOUNTS, id, failingFile));

        assertEquals(500, exception.getStatusCode());
        assertEquals("error.avatar.storage", exception.getError().key());
        assertEquals(0xFF0000, storedColor("accounts"));
        try (var files = Files.list(location.resolve("accounts"))) {
            assertEquals(List.of(avatarPath("accounts")), files.toList());
        }
    }

    @Test
    @DisplayName("Should return 400 when the file content is not a PNG image")
    void testUploadInvalidContent() {
        var exception = assertThrows(ApiException.class,
            () -> service.upload(AvatarEntity.ACCOUNTS, id, file("me.png", new byte[] {1, 2, 3})));

        assertEquals(400, exception.getStatusCode());
        assertEquals("error.avatar.content.invalid", exception.getError().key());
        assertFalse(Files.exists(avatarPath("accounts")));
    }

    @Test
    @DisplayName("Should delete an existing avatar")
    void testDelete() throws IOException {
        service.upload(AvatarEntity.ACCOUNTS, id, file("me.png", pngBytes(0xFF0000)));

        service.delete(AvatarEntity.ACCOUNTS, id);

        assertFalse(Files.exists(avatarPath("accounts")));
    }

    @Test
    @DisplayName("Should succeed when there is no avatar to delete")
    void testDeleteMissingAvatar() {
        assertDoesNotThrow(() -> service.delete(AvatarEntity.ORGANIZATIONAL_UNITS, id));
    }

    @Test
    @DisplayName("Should return 500 when the avatar cannot be deleted")
    void testDeleteStorageFailure() throws IOException {
        Files.createDirectories(avatarPath("accounts").resolve("child"));

        var exception = assertThrows(ApiException.class, () -> service.delete(AvatarEntity.ACCOUNTS, id));

        assertEquals(500, exception.getStatusCode());
        assertEquals("error.avatar.deletion", exception.getError().key());
    }
}
