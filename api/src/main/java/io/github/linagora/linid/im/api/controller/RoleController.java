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

import io.github.linagora.linid.im.api.model.role.RoleDTO;
import io.github.linagora.linid.im.api.model.role.RoleMapper;
import io.github.linagora.linid.im.api.model.role.RoleRecord;
import io.github.linagora.linid.im.api.model.role.RoleViewDTO;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.RoleViewQueryFilterDto;
import io.github.linagora.linid.im.api.service.RoleService;
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
 * REST controller for functional role management endpoints.
 *
 * <p>Provides CRUD operations for functional roles with pagination and
 * filtering support via {@code spring-query-filter}.</p>
 */
@Slf4j
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Functional role management endpoints")
public class RoleController {

    /**
     * Service handling functional role business logic.
     */
    private final RoleService roleService;

    /**
     * Mapper for entity-to-DTO conversion.
     */
    private final RoleMapper roleMapper;

    /**
     * Resolver for paginated response HTTP status.
     */
    private final PagedResponseStatusResolver pagedResponseStatusResolver;

    /**
     * Creates a new functional role.
     *
     * @param userPrincipal the authenticated user
     * @param role          the role creation record with validated fields
     * @return the created role with HTTP 201 status
     */
    @PostMapping
    @Operation(summary = "Create a new functional role")
    @ApiResponse(responseCode = "201", description = "Role successfully created")
    @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    public ResponseEntity<RoleDTO> create(
            @AuthenticationPrincipal final UserPrincipal userPrincipal,
            @Valid @RequestBody final RoleRecord role) {
        log.info("[{}] Received POST request to create role with {}", userPrincipal.getEmail(), role);
        var entity = roleService.create(userPrincipal, role);
        return ResponseEntity.status(HttpStatus.CREATED).body(roleMapper.toDTO(entity));
    }

    /**
     * Retrieves a paginated and optionally filtered list of functional roles.
     *
     * @param userPrincipal the authenticated user
     * @param filters       generated filter DTO from query parameters
     * @param pageable      pagination parameters
     * @return a page of role view DTOs
     */
    @GetMapping
    @Operation(summary = "Get all functional roles with pagination and filtering")
    @ApiResponse(responseCode = "200", description = "Full list of functional roles")
    @ApiResponse(responseCode = "206",
            description = "Partial list of functional roles (more pages available)")
    public ResponseEntity<Page<RoleViewDTO>> findAll(
            @AuthenticationPrincipal final UserPrincipal userPrincipal,
            final RoleViewQueryFilterDto filters,
            final Pageable pageable) {
        log.info("[{}] Received GET request to list roles with filters {} and pageable {}",
                userPrincipal.getEmail(), filters, pageable);
        var page = roleService.findAll(userPrincipal, filters, pageable)
                .map(roleMapper::toDTO);
        return pagedResponseStatusResolver.resolve(page);
    }

    /**
     * Retrieves a functional role by its unique identifier.
     *
     * @param userPrincipal the authenticated user
     * @param id            the role UUID
     * @return the role view DTO
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get a functional role by ID")
    @ApiResponse(responseCode = "200", description = "Role found")
    @ApiResponse(responseCode = "404", description = "Role not found", content = @Content)
    public ResponseEntity<RoleViewDTO> findById(
            @AuthenticationPrincipal final UserPrincipal userPrincipal,
            @PathVariable final UUID id) {
        log.info("[{}] Received GET request for role {}", userPrincipal.getEmail(), id);
        var entity = roleService.findById(userPrincipal, id);
        return ResponseEntity.ok(roleMapper.toDTO(entity));
    }

    /**
     * Updates a functional role.
     *
     * @param userPrincipal the authenticated user
     * @param id            the role UUID
     * @param role          the role update record with validated fields
     * @return the refreshed role view
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update a functional role")
    @ApiResponse(responseCode = "200", description = "Role successfully updated")
    @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    @ApiResponse(responseCode = "404", description = "Role not found", content = @Content)
    public ResponseEntity<RoleViewDTO> update(
            @AuthenticationPrincipal final UserPrincipal userPrincipal,
            @PathVariable final UUID id,
            @Valid @RequestBody final RoleRecord role) {
        log.info("[{}] Received PUT request to update role {} with {}",
                userPrincipal.getEmail(), id, role);
        var view = roleService.update(userPrincipal, id, role);
        return ResponseEntity.ok(roleMapper.toDTO(view));
    }

    /**
     * Deletes a functional role by its unique identifier.
     *
     * @param userPrincipal the authenticated user
     * @param id            the role UUID
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a functional role by ID")
    @ApiResponse(responseCode = "204", description = "Role successfully deleted")
    @ApiResponse(responseCode = "400", description = "Role held by at least one account", content = @Content)
    @ApiResponse(responseCode = "404", description = "Role not found", content = @Content)
    public ResponseEntity<Void> deleteById(
            @AuthenticationPrincipal final UserPrincipal userPrincipal,
            @PathVariable final UUID id) {
        log.info("[{}] Received DELETE request for role {}", userPrincipal.getEmail(), id);
        roleService.deleteById(userPrincipal, id);
        return ResponseEntity.noContent().build();
    }
}
