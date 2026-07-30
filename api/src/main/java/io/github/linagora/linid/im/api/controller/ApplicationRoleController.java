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

import io.github.linagora.linid.im.api.model.application.role.ApplicationRoleDTO;
import io.github.linagora.linid.im.api.model.application.role.ApplicationRoleMapper;
import io.github.linagora.linid.im.api.model.application.role.ApplicationRoleRecord;
import io.github.linagora.linid.im.api.model.application.role.ApplicationRoleViewDTO;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.ApplicationRoleViewQueryFilterDto;
import io.github.linagora.linid.im.api.service.ApplicationRoleService;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
 * REST controller for application role management endpoints.
 *
 * <p>Provides CRUD operations for the roles of a given application, with pagination and filtering
 * support via {@code spring-query-filter}. All endpoints are nested under the owning application.</p>
 */
@Slf4j
@RestController
@RequestMapping("/applications/{applicationId}/roles")
@RequiredArgsConstructor
@Tag(name = "Application Roles", description = "Application role management endpoints")
public class ApplicationRoleController {

    /**
     * Service handling application role business logic.
     */
    private final ApplicationRoleService applicationRoleService;

    /**
     * Mapper for entity-to-DTO conversion.
     */
    private final ApplicationRoleMapper applicationRoleMapper;

    /**
     * Resolver for paginated response HTTP status.
     */
    private final PagedResponseStatusResolver pagedResponseStatusResolver;

    /**
     * Creates a new role for the given application.
     *
     * @param userPrincipal the authenticated user
     * @param applicationId the owning application UUID
     * @param role          the role creation record with validated fields
     * @return the created role with HTTP 201 status
     */
    @PostMapping
    @Operation(summary = "Create a new application role")
    @ApiResponse(responseCode = "201", description = "Application role successfully created")
    @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    @ApiResponse(responseCode = "404", description = "Application not found", content = @Content)
    public ResponseEntity<ApplicationRoleDTO> create(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID applicationId,
        @Valid @RequestBody final ApplicationRoleRecord role) {
        log.info("[{}] Received POST request to create role for application {} with {}",
            userPrincipal.getEmail(), applicationId, role);
        var entity = applicationRoleService.create(userPrincipal, applicationId, role);
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationRoleMapper.toDTO(entity));
    }

    /**
     * Retrieves a paginated and optionally filtered list of roles for the given application.
     *
     * <p>Roles are sorted by {@code name} ascending by default.</p>
     *
     * @param userPrincipal the authenticated user
     * @param applicationId the owning application UUID
     * @param filters       generated filter DTO from query parameters
     * @param pageable      pagination parameters
     * @return a page of application role view DTOs
     */
    @GetMapping
    @Operation(summary = "Get all roles of an application with pagination and filtering")
    @ApiResponse(responseCode = "200", description = "Full list of application roles")
    @ApiResponse(responseCode = "206", description = "Partial list of application roles (more pages available)")
    @ApiResponse(responseCode = "404", description = "Application not found", content = @Content)
    public ResponseEntity<Page<ApplicationRoleViewDTO>> findAll(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID applicationId,
        final ApplicationRoleViewQueryFilterDto filters,
        @PageableDefault(sort = "name", direction = Sort.Direction.ASC) final Pageable pageable) {
        log.info("[{}] Received GET request to list roles of application {} with filters {} and pageable {}",
            userPrincipal.getEmail(), applicationId, filters, pageable);
        var page = applicationRoleService.findAll(userPrincipal, applicationId, filters, pageable)
            .map(applicationRoleMapper::toDTO);
        return pagedResponseStatusResolver.resolve(page);
    }

    /**
     * Retrieves a role of the given application by its unique identifier.
     *
     * @param userPrincipal the authenticated user
     * @param applicationId the owning application UUID
     * @param roleId        the role UUID
     * @return the application role view DTO
     */
    @GetMapping("/{roleId}")
    @Operation(summary = "Get an application role by ID")
    @ApiResponse(responseCode = "200", description = "Application role found")
    @ApiResponse(responseCode = "404", description = "Application role not found", content = @Content)
    public ResponseEntity<ApplicationRoleViewDTO> findById(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID applicationId,
        @PathVariable final UUID roleId) {
        log.info("[{}] Received GET request for role {} of application {}",
            userPrincipal.getEmail(), roleId, applicationId);
        var entity = applicationRoleService.findViewById(userPrincipal, applicationId, roleId);
        return ResponseEntity.ok(applicationRoleMapper.toDTO(entity));
    }

    /**
     * Updates a role of the given application.
     *
     * @param userPrincipal the authenticated user
     * @param applicationId the owning application UUID
     * @param roleId        the role UUID
     * @param role          the update record with validated fields
     * @return the updated role
     */
    @PutMapping("/{roleId}")
    @Operation(summary = "Update an application role")
    @ApiResponse(responseCode = "200", description = "Application role successfully updated")
    @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    @ApiResponse(responseCode = "404", description = "Application role not found", content = @Content)
    public ResponseEntity<ApplicationRoleDTO> update(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID applicationId,
        @PathVariable final UUID roleId,
        @Valid @RequestBody final ApplicationRoleRecord role) {
        log.info("[{}] Received PUT request to update role {} of application {} with {}",
            userPrincipal.getEmail(), roleId, applicationId, role);
        var entity = applicationRoleService.update(userPrincipal, applicationId, roleId, role);
        return ResponseEntity.ok(applicationRoleMapper.toDTO(entity));
    }

    /**
     * Deletes a role of the given application by its unique identifier.
     *
     * @param userPrincipal the authenticated user
     * @param applicationId the owning application UUID
     * @param roleId        the role UUID
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{roleId}")
    @Operation(summary = "Delete an application role by ID")
    @ApiResponse(responseCode = "204", description = "Application role successfully deleted")
    @ApiResponse(responseCode = "404", description = "Application role not found", content = @Content)
    public ResponseEntity<Void> deleteById(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID applicationId,
        @PathVariable final UUID roleId) {
        log.info("[{}] Received DELETE request for role {} of application {}",
            userPrincipal.getEmail(), roleId, applicationId);
        applicationRoleService.deleteById(userPrincipal, applicationId, roleId);
        return ResponseEntity.noContent().build();
    }
}
