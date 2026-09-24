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

package io.github.linagora.linid.im.api.controller;

import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.service.AccountService;
import io.github.linagora.linid.im.api.service.ApplicationService;
import io.github.linagora.linid.im.api.service.GroupService;
import io.github.linagora.linid.im.api.service.OrganizationalUnitService;
import io.github.linagora.linid.im.api.service.AvatarService;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for the avatar images of accounts, applications, organizational units and groups.
 *
 * <p>The images are stored on the filesystem and served by the front-end; this controller only
 * uploads and deletes them.</p>
 */
@Slf4j
@RestController
@RequestMapping("/avatars")
@RequiredArgsConstructor
@Tag(name = "Avatars", description = "Avatar image management endpoints")
public class AvatarController {

    /**
     * Service handling avatar storage.
     */
    private final AvatarService avatarService;

    /**
     * Service checking the existence of accounts.
     */
    private final AccountService accountService;

    /**
     * Service checking the existence of applications.
     */
    private final ApplicationService applicationService;

    /**
     * Service checking the existence of organizational units.
     */
    private final OrganizationalUnitService organizationalUnitService;

    /**
     * Service checking the existence of groups.
     */
    private final GroupService groupService;

    /**
     * Uploads the avatar image of an entity, replacing any existing one.
     *
     * @param userPrincipal the authenticated user
     * @param entity        the entity type: {@code accounts}, {@code applications}, {@code organizational-units}
     *                      or {@code groups}
     * @param id            the entity UUID
     * @param file          the image, sent as the {@code file} part of a multipart request
     * @return HTTP 204 status
     */
    @PostMapping(value = "/{entity}/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload the avatar image of an entity")
    @ApiResponse(responseCode = "204", description = "Avatar successfully stored")
    @ApiResponse(responseCode = "400", description = "Unsupported entity type or invalid file", content = @Content)
    @ApiResponse(responseCode = "404", description = "Entity not found", content = @Content)
    public ResponseEntity<Void> upload(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final String entity,
        @PathVariable final UUID id,
        @RequestParam("file") final MultipartFile file) {
        log.info("[{}] Received POST request to upload the avatar of {} {}", userPrincipal.getEmail(), entity, id);
        ensureEntityExists(userPrincipal, entity, id);
        avatarService.upload(entity, id, file);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deletes the avatar image of an entity, if any.
     *
     * @param userPrincipal the authenticated user
     * @param entity        the entity type: {@code accounts}, {@code applications}, {@code organizational-units}
     *                      or {@code groups}
     * @param id            the entity UUID
     * @return HTTP 204 status
     */
    @DeleteMapping("/{entity}/{id}")
    @Operation(summary = "Delete the avatar image of an entity")
    @ApiResponse(responseCode = "204", description = "Avatar deleted, or no avatar to delete")
    @ApiResponse(responseCode = "400", description = "Unsupported entity type", content = @Content)
    @ApiResponse(responseCode = "404", description = "Entity not found", content = @Content)
    public ResponseEntity<Void> delete(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final String entity,
        @PathVariable final UUID id) {
        log.info("[{}] Received DELETE request to delete the avatar of {} {}", userPrincipal.getEmail(), entity, id);
        ensureEntityExists(userPrincipal, entity, id);
        avatarService.delete(entity, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Checks that the entity type is supported and that the target entity exists, delegating the 404
     * {@link ApiException} to the service of that type.
     *
     * @param userPrincipal the authenticated user
     * @param entity        the entity type
     * @param id            the entity UUID
     */
    private void ensureEntityExists(final UserPrincipal userPrincipal, final String entity, final UUID id) {
        switch (entity) {
            case "accounts" -> accountService.existsById(userPrincipal, id);
            case "applications" -> applicationService.existsById(userPrincipal, id);
            case "organizational-units" -> organizationalUnitService.existsById(userPrincipal, id);
            case "groups" -> groupService.existsById(userPrincipal, id);
            default -> throw new ApiException(HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.avatar.entity.unsupported", Map.of("entity", entity)));
        }
    }
}
