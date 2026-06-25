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
import io.github.linagora.linid.im.api.model.user.preference.UserPreferenceDTO;
import io.github.linagora.linid.im.api.model.user.preference.UserPreferenceMapper;
import io.github.linagora.linid.im.api.model.user.preference.UserPreferenceRecord;
import io.github.linagora.linid.im.api.persistence.model.UserPreference;
import io.github.linagora.linid.im.api.service.UserPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for user preference endpoints.
 *
 * <p>Exposes a small key/value preference store scoped to the authenticated user. The owning user
 * is always resolved from the security context ({@code @AuthenticationPrincipal}); it is never taken
 * from the request body or path, so a user can only act on their own preferences.</p>
 */
@Slf4j
@RestController
@RequestMapping("/user-preferences")
@RequiredArgsConstructor
@Tag(name = "User Preferences", description = "User preference management endpoints")
public class UserPreferenceController {

    /**
     * Service handling user preference business logic.
     */
    private final UserPreferenceService userPreferenceService;

    /**
     * Mapper for entity-to-DTO conversion.
     */
    private final UserPreferenceMapper userPreferenceMapper;

    /**
     * Creates a new user preference or updates the value of an existing one (upsert).
     *
     * @param userPrincipal  the authenticated user
     * @param userPreference the user preference key/value record with validated fields
     * @return the created or updated user preference with HTTP 201 status
     */
    @PostMapping
    @Operation(summary = "Create or update a user preference")
    @ApiResponse(responseCode = "201", description = "User preference successfully created or updated")
    @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    public ResponseEntity<UserPreferenceDTO> save(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @Valid @RequestBody final UserPreferenceRecord userPreference) {
        log.info("[{}] Received POST request to save user preference {}", userPrincipal.getEmail(), userPreference);
        var entity = userPreferenceService.save(userPrincipal, userPreference);
        return ResponseEntity.status(HttpStatus.CREATED).body(userPreferenceMapper.toDTO(entity));
    }

    /**
     * Deletes a user preference identified by its key for the authenticated user.
     *
     * @param userPrincipal the authenticated user
     * @param key           the key of the user preference to delete
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{key}")
    @Operation(summary = "Delete a user preference by key")
    @ApiResponse(responseCode = "204", description = "User preference successfully deleted")
    @ApiResponse(responseCode = "404", description = "User preference not found", content = @Content)
    public ResponseEntity<Void> delete(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final String key) {
        log.info("[{}] Received DELETE request for user preference {}", userPrincipal.getEmail(), key);
        userPreferenceService.delete(userPrincipal, key);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves all user preferences of the authenticated user as a {@code {key: value}} map.
     *
     * @param userPrincipal the authenticated user
     * @return the user preferences aggregated into a map, empty if none exist
     */
    @GetMapping
    @Operation(summary = "Get all user preferences")
    @ApiResponse(responseCode = "200", description = "Map of the user preferences")
    public ResponseEntity<Map<String, String>> findAll(
        @AuthenticationPrincipal final UserPrincipal userPrincipal) {
        log.info("[{}] Received GET request to list user preferences", userPrincipal.getEmail());
        Map<String, String> userPreferences = userPreferenceService.findAll(userPrincipal).stream()
                .collect(Collectors.toMap(UserPreference::getKey, UserPreference::getValue));
        return ResponseEntity.ok(userPreferences);
    }
}
