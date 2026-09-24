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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.linagora.linid.im.corelib.exception.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
        service = new AvatarServiceImpl(location.toString(), "png");
        id = UUID.randomUUID();
    }

    private MockMultipartFile file(final String name, final byte[] content) {
        return new MockMultipartFile("file", name, "image/png", content);
    }

    private Path avatarPath(final String entity) {
        return location.resolve(entity).resolve(id + ".png");
    }

    @Test
    @DisplayName("Should store the avatar of an existing account")
    void testUploadAccount() throws IOException {
        service.upload("accounts", id, file("me.png", new byte[] {1, 2, 3}));

        assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(avatarPath("accounts")));
    }

    @Test
    @DisplayName("Should store the avatar of an existing application")
    void testUploadApplication() {
        service.upload("applications", id, file("app.PNG", new byte[] {1}));

        assertTrue(Files.exists(avatarPath("applications")));
    }

    @Test
    @DisplayName("Should store the avatar of an existing group")
    void testUploadGroup() {
        service.upload("groups", id, file("group.png", new byte[] {1}));

        assertTrue(Files.exists(avatarPath("groups")));
    }

    @Test
    @DisplayName("Should store the avatar of an existing organizational unit")
    void testUploadOrganizationalUnit() {
        service.upload("organizational-units", id, file("ou.png", new byte[] {1}));

        assertTrue(Files.exists(avatarPath("organizational-units")));
    }

    @Test
    @DisplayName("Should replace an existing avatar")
    void testUploadReplacesExistingAvatar() throws IOException {
        service.upload("accounts", id, file("old.png", new byte[] {1, 2, 3}));

        service.upload("accounts", id, file("new.png", new byte[] {9}));

        assertArrayEquals(new byte[] {9}, Files.readAllBytes(avatarPath("accounts")));
    }

    @Test
    @DisplayName("Should return 400 when the file is empty")
    void testUploadEmptyFile() {
        var exception = assertThrows(ApiException.class,
            () -> service.upload("accounts", id, file("me.png", new byte[0])));

        assertEquals(400, exception.getStatusCode());
        assertEquals("error.avatar.file.empty", exception.getError().key());
    }

    @Test
    @DisplayName("Should return 400 when the file extension is not the configured one")
    void testUploadInvalidExtension() {
        var exception = assertThrows(ApiException.class,
            () -> service.upload("accounts", id, file("me.jpg", new byte[] {1})));

        assertEquals(400, exception.getStatusCode());
        assertEquals("error.avatar.extension.invalid", exception.getError().key());
        assertEquals(Map.of("extension", "png"), exception.getError().context());
        assertFalse(Files.exists(avatarPath("accounts")));
    }

    @Test
    @DisplayName("Should return 400 when the file has no extension")
    void testUploadMissingExtension() {
        var exception = assertThrows(ApiException.class,
            () -> service.upload("accounts", id, file("me", new byte[] {1})));

        assertEquals("error.avatar.extension.invalid", exception.getError().key());
    }


    @Test
    @DisplayName("Should return 500 when the file cannot be stored")
    void testUploadStorageFailure() throws IOException {
        Files.createFile(location.resolve("accounts"));

        var exception = assertThrows(ApiException.class,
            () -> service.upload("accounts", id, file("me.png", new byte[] {1})));

        assertEquals(500, exception.getStatusCode());
        assertEquals("error.avatar.storage", exception.getError().key());
    }

    @Test
    @DisplayName("Should keep the previous avatar and remove the temporary file when the upload fails")
    void testUploadFailureKeepsPreviousAvatar() throws IOException {
        service.upload("accounts", id, file("old.png", new byte[] {1, 2, 3}));
        var failingFile = new MockMultipartFile("file", "new.png", "image/png", new byte[] {9}) {
            @Override
            public InputStream getInputStream() {
                return new InputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new IOException("Connection reset during the upload");
                    }
                };
            }
        };

        var exception = assertThrows(ApiException.class,
            () -> service.upload("accounts", id, failingFile));

        assertEquals(500, exception.getStatusCode());
        assertEquals("error.avatar.storage", exception.getError().key());
        assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(avatarPath("accounts")));
        try (var files = Files.list(location.resolve("accounts"))) {
            assertEquals(List.of(avatarPath("accounts")), files.toList());
        }
    }

    @Test
    @DisplayName("Should delete an existing avatar")
    void testDelete() throws IOException {
        service.upload("accounts", id, file("me.png", new byte[] {1}));

        service.delete("accounts", id);

        assertFalse(Files.exists(avatarPath("accounts")));
    }

    @Test
    @DisplayName("Should succeed when there is no avatar to delete")
    void testDeleteMissingAvatar() {
        service.delete("organizational-units", id);

    }

    @Test
    @DisplayName("Should return 500 when the avatar cannot be deleted")
    void testDeleteStorageFailure() throws IOException {
        Files.createDirectories(avatarPath("accounts").resolve("child"));

        var exception = assertThrows(ApiException.class, () -> service.delete("accounts", id));

        assertEquals(500, exception.getStatusCode());
        assertEquals("error.avatar.deletion", exception.getError().key());
    }
}
