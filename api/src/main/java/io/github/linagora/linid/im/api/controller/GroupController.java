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

import io.github.linagora.linid.im.api.model.group.GroupDTO;
import io.github.linagora.linid.im.api.model.group.GroupMapper;
import io.github.linagora.linid.im.api.model.group.GroupRecord;
import io.github.linagora.linid.im.api.model.group.GroupViewDTO;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.GroupViewQueryFilterDto;
import io.github.linagora.linid.im.api.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for group management endpoints.
 *
 * <p>Provides CRUD operations for groups with pagination and filtering
 * support via {@code spring-query-filter}.</p>
 */
@Slf4j
@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
@Tag(name = "Groups", description = "Group management endpoints")
public class GroupController {

    /**
     * Service handling group business logic.
     */
    private final GroupService groupService;

    /**
     * Mapper for entity-to-DTO conversion.
     */
    private final GroupMapper groupMapper;

    /**
     * Resolver for paginated response HTTP status.
     */
    private final PagedResponseStatusResolver pagedResponseStatusResolver;

    /**
     * Creates a new group.
     *
     * @param userPrincipal the authenticated user
     * @param group         the group creation record with validated fields
     * @return the created group with HTTP 201 status
     */
    @PostMapping
    @Operation(summary = "Create a new group")
    @ApiResponse(responseCode = "201", description = "Group successfully created")
    @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    @ApiResponse(responseCode = "404", description = "Referenced parent group, organizational unit or application"
        + " not found", content = @Content)
    public ResponseEntity<GroupDTO> create(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @Valid @RequestBody final GroupRecord group) {
        log.info("[{}] Received POST request to create group with {}", userPrincipal.getEmail(), group);
        var entity = groupService.create(userPrincipal, group);
        return ResponseEntity.status(HttpStatus.CREATED).body(groupMapper.toDTO(entity));
    }

    /**
     * Retrieves a paginated and optionally filtered list of groups.
     *
     * @param userPrincipal the authenticated user
     * @param filters       generated filter DTO from query parameters
     * @param pageable      pagination parameters
     * @return a page of group view DTOs
     */
    @GetMapping
    @Operation(summary = "Get all groups with pagination and filtering")
    @ApiResponse(responseCode = "200", description = "Full list of groups")
    @ApiResponse(responseCode = "206", description = "Partial list of groups (more pages available)")
    public ResponseEntity<Page<GroupViewDTO>> findAll(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        final GroupViewQueryFilterDto filters,
        final Pageable pageable) {
        log.info("[{}] Received GET request to list groups with filters {} and pageable {}",
            userPrincipal.getEmail(), filters, pageable);
        var page = groupService.findAll(userPrincipal, filters, pageable).map(groupMapper::toDTO);
        return pagedResponseStatusResolver.resolve(page);
    }

    /**
     * Retrieves a group by its unique identifier.
     *
     * @param userPrincipal the authenticated user
     * @param id            the group UUID
     * @return the group view DTO
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a group by ID")
    @ApiResponse(responseCode = "200", description = "Group found")
    @ApiResponse(responseCode = "404", description = "Group not found", content = @Content)
    public ResponseEntity<GroupViewDTO> findById(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID id) {
        log.info("[{}] Received GET request for group {}", userPrincipal.getEmail(), id);
        var entity = groupService.findViewById(userPrincipal, id);
        return ResponseEntity.ok(groupMapper.toDTO(entity));
    }

    /**
     * Updates a group.
     *
     * @param userPrincipal the authenticated user
     * @param id            the group UUID
     * @param group         the update record with validated fields
     * @return the updated group
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update a group")
    @ApiResponse(responseCode = "200", description = "Group successfully updated")
    @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    @ApiResponse(responseCode = "404", description = "Group, parent group, organizational unit or application not"
        + " found", content = @Content)
    public ResponseEntity<GroupDTO> update(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID id,
        @Valid @RequestBody final GroupRecord group) {
        log.info("[{}] Received PUT request to update group {} with {}", userPrincipal.getEmail(), id, group);
        var entity = groupService.update(userPrincipal, id, group);
        return ResponseEntity.ok(groupMapper.toDTO(entity));
    }

    /**
     * Deletes a group by its unique identifier.
     *
     * @param userPrincipal the authenticated user
     * @param id            the group UUID
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a group by ID")
    @ApiResponse(responseCode = "204", description = "Group successfully deleted")
    @ApiResponse(responseCode = "404", description = "Group not found", content = @Content)
    public ResponseEntity<Void> deleteById(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID id) {
        log.info("[{}] Received DELETE request for group {}", userPrincipal.getEmail(), id);
        groupService.deleteById(userPrincipal, id);
        return ResponseEntity.noContent().build();
    }
}
