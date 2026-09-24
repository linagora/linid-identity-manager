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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Default implementation of {@link AvatarService}.
 *
 * <p>Avatars are stored as {@code {avatar.location}/{entity}/{id}.{avatar.extension}}. The uploaded file must
 * carry the configured extension and must not exceed the configured maximum size, in megabytes. The target
 * entity must exist: its existence is checked through the service of its type.</p>
 */
@Slf4j
@Service
public class AvatarServiceImpl implements AvatarService {

    /**
     * Root directory of the avatar files.
     */
    private final Path location;

    /**
     * Only allowed avatar file extension.
     */
    private final String extension;

    /**
     * Creates the service.
     *
     * @param location  root directory of the avatar files
     * @param extension only allowed avatar file extension
     */
    public AvatarServiceImpl(
        @Value("${avatar.location}") final String location,
        @Value("${avatar.extension}") final String extension) {
        this.location = Path.of(location).normalize();
        this.extension = extension;
    }

    @Override
    public void upload(final String entity, final UUID id, final MultipartFile file) {
        validateFile(file);

        var target = resolve(entity, id);
        Path temporary = null;

        try {
            Files.createDirectories(target.getParent());
            temporary = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".part");

            try (var stream = file.getInputStream()) {
                Files.copy(stream, temporary, StandardCopyOption.REPLACE_EXISTING);
            }

            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            deleteQuietly(temporary);
            throw filesystemException("error.avatar.storage", id, e);
        }
    }

    @Override
    public void delete(final String entity, final UUID id) {
        try {
            Files.deleteIfExists(resolve(entity, id));
        } catch (IOException e) {
            throw filesystemException("error.avatar.deletion", id, e);
        }
    }

    @Override
    public void deleteQuietly(final String entity, final UUID id) {
        try {
            delete(entity, id);
        } catch (ApiException e) {
            log.warn("Unable to delete the avatar of {} {}", entity, id, e);
        }
    }

    /**
     * Checks that the uploaded file is not empty and carries the configured extension. The file size is bounded by
     * the multipart limits of the servlet container, derived from {@code avatar.max-size}, so an oversized upload is
     * rejected before reaching this service.
     *
     * @param file the uploaded image
     */
    private void validateFile(final MultipartFile file) {
        if (file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), I18nMessage.of("error.avatar.file.empty"));
        }

        var actualExtension = StringUtils.getFilenameExtension(file.getOriginalFilename());

        if (!extension.equalsIgnoreCase(actualExtension)) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.avatar.extension.invalid", Map.of("extension", extension)));
        }
    }

    /**
     * Resolves the path of the avatar file of an entity, refusing any path leaving the avatar directory.
     *
     * <p>The entity type is restricted to the supported keys and the identifier is a UUID, so the resolved path
     * always stays under the directory: the check is a defence in depth, and the sanitizer static analysis needs to
     * see that the path built from the request cannot escape.</p>
     *
     * @param entity the entity type
     * @param id     the entity UUID
     * @return the avatar file path
     */
    private Path resolve(final String entity, final UUID id) {
        var target = location.resolve(entity).resolve(id + "." + extension).normalize();

        if (!target.startsWith(location)) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.avatar.entity.unsupported", Map.of("entity", entity)));
        }

        return target;
    }

    /**
     * Deletes a file, ignoring any failure: it is only called while handling another failure.
     *
     * @param path the file to delete, possibly null
     */
    private static void deleteQuietly(final Path path) {
        if (path == null) {
            return;
        }

        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Unable to delete the temporary avatar file {}", path, e);
        }
    }

    /**
     * Wraps a filesystem failure into an internal server error.
     *
     * @param key   the error message key
     * @param id    the entity UUID
     * @param cause the filesystem failure
     * @return the exception to throw
     */
    private static ApiException filesystemException(final String key, final UUID id, final IOException cause) {
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR.value(),
            I18nMessage.of(key, Map.of("id", id.toString())), cause);
    }
}
