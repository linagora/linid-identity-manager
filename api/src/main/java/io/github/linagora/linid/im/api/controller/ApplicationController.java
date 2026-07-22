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

import io.github.linagora.linid.im.api.model.application.ApplicationDTO;
import io.github.linagora.linid.im.api.model.application.ApplicationMapper;
import io.github.linagora.linid.im.api.model.application.ApplicationRecord;
import io.github.linagora.linid.im.api.model.application.ApplicationRoleDTO;
import io.github.linagora.linid.im.api.model.application.ApplicationRoleRecord;
import io.github.linagora.linid.im.api.model.application.ApplicationViewDTO;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.ApplicationViewQueryFilterDto;
import io.github.linagora.linid.im.api.service.ApplicationService;
import io.github.linagora.linid.im.api.service.OpaApplicationDeployerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for application management endpoints.
 *
 * <p>Provides CRUD operations for applications with pagination and filtering
 * support via {@code spring-query-filter}.</p>
 */
@Slf4j
@RestController
@RequestMapping("/applications")
@RequiredArgsConstructor
@Tag(name = "Applications", description = "Application management endpoints")
public class ApplicationController {

    /**
     * Service handling application business logic.
     */
    private final ApplicationService applicationService;

    /**
     * Mapper for entity-to-DTO conversion.
     */
    private final ApplicationMapper applicationMapper;

    /**
     * Resolver for paginated response HTTP status.
     */
    private final PagedResponseStatusResolver pagedResponseStatusResolver;

    /**
     * Service for deploying OPA application policies.
     */
    private final OpaApplicationDeployerService opaApplicationDeployerService;

    /**
     * Creates a new application.
     *
     * @param userPrincipal the authenticated user
     * @param application   the application creation record with validated fields
     * @return the created application with HTTP 201 status
     */
    @PostMapping
    @Operation(summary = "Create a new application")
    @ApiResponse(responseCode = "201", description = "Application successfully created")
    @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    public ResponseEntity<ApplicationDTO> create(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @Valid @RequestBody final ApplicationRecord application) {
        log.info("[{}] Received POST request to create application with {}", userPrincipal.getEmail(),
            application);
        var entity = applicationService.create(userPrincipal, application);
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationMapper.toDTO(entity));
    }

    /**
     * Retrieves a paginated and optionally filtered list of applications.
     *
     * @param userPrincipal the authenticated user
     * @param filters       generated filter DTO from query parameters
     * @param pageable      pagination parameters
     * @return a page of application view DTOs
     */
    @GetMapping
    @Operation(summary = "Get all applications with pagination and filtering")
    @ApiResponse(responseCode = "200", description = "Full list of applications")
    @ApiResponse(responseCode = "206", description = "Partial list of applications (more pages available)")
    public ResponseEntity<Page<ApplicationViewDTO>> findAll(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        final ApplicationViewQueryFilterDto filters,
        final Pageable pageable) {
        log.info("[{}] Received GET request to list applications with filters {} and pageable {}",
            userPrincipal.getEmail(), filters, pageable);
        var page = applicationService.findAll(userPrincipal, filters, pageable).map(applicationMapper::toDTO);
        return pagedResponseStatusResolver.resolve(page);
    }

    /**
     * Retrieves an application by its unique identifier.
     *
     * @param userPrincipal the authenticated user
     * @param id            the application UUID
     * @return the application view DTO
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get an application by ID")
    @ApiResponse(responseCode = "200", description = "Application found")
    @ApiResponse(responseCode = "404", description = "Application not found", content = @Content)
    public ResponseEntity<ApplicationViewDTO> findById(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID id) {
        log.info("[{}] Received GET request for application {}", userPrincipal.getEmail(), id);
        var entity = applicationService.findViewById(userPrincipal, id);
        return ResponseEntity.ok(applicationMapper.toDTO(entity));
    }

    /**
     * Updates an application.
     *
     * @param userPrincipal the authenticated user
     * @param id            the application UUID
     * @param application   the update record with validated fields
     * @return the updated application
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update an application")
    @ApiResponse(responseCode = "200", description = "Application successfully updated")
    @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    @ApiResponse(responseCode = "404", description = "Application not found", content = @Content)
    public ResponseEntity<ApplicationDTO> update(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID id,
        @Valid @RequestBody final ApplicationRecord application) {
        log.info("[{}] Received PUT request to update application {} with {}",
            userPrincipal.getEmail(), id, application);
        var entity = applicationService.update(userPrincipal, id, application);
        return ResponseEntity.ok(applicationMapper.toDTO(entity));
    }

    /**
     * Retrieves the roles of an application.
     *
     * @param userPrincipal the authenticated user
     * @param id            the application UUID
     * @return the full list of roles of the application
     */
    @GetMapping("/{id}/roles")
    @Operation(summary = "Retrieve the roles of an application")
    @ApiResponse(responseCode = "200", description = "Application roles successfully retrieved")
    @ApiResponse(responseCode = "404", description = "Application not found", content = @Content)
    public ResponseEntity<List<ApplicationRoleDTO>> getRoles(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID id) {
        log.info("[{}] Received GET request to retrieve roles of application {}",
            userPrincipal.getEmail(), id);
        var entity = applicationService.findById(userPrincipal, id);
        return ResponseEntity.ok(Objects.requireNonNullElse(entity.getRoles(), List.of()));
    }

    /**
     * Updates the roles of an application.
     *
     * <p>The response contains the updated full list of roles, which is the single source of truth for the caller:
     * no additional fetch is required after the update.</p>
     *
     * @param userPrincipal the authenticated user
     * @param id            the application UUID
     * @param roles         the new full list of roles (replaces the existing one)
     * @return the updated full list of roles of the application
     */
    @PutMapping("/{id}/roles")
    @Operation(summary = "Update the roles of an application")
    @ApiResponse(responseCode = "200", description = "Application roles successfully updated")
    @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    @ApiResponse(responseCode = "404", description = "Application not found", content = @Content)
    public ResponseEntity<List<ApplicationRoleDTO>> updateRoles(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID id,
        @Valid @RequestBody @NotNull final List<ApplicationRoleRecord> roles) {
        log.info("[{}] Received PUT request to update roles of application {} with {}",
            userPrincipal.getEmail(), id, roles);
        var entity = applicationService.updateRoles(userPrincipal, id, roles);
        return ResponseEntity.ok(Objects.requireNonNullElse(entity.getRoles(), List.of()));
    }

    /**
     * Deletes an application by its unique identifier.
     *
     * @param userPrincipal the authenticated user
     * @param id            the application UUID
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an application by ID")
    @ApiResponse(responseCode = "204", description = "Application successfully deleted")
    @ApiResponse(responseCode = "404", description = "Application not found", content = @Content)
    public ResponseEntity<Void> deleteById(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID id) {
        log.info("[{}] Received DELETE request for application {}", userPrincipal.getEmail(), id);
        applicationService.deleteById(userPrincipal, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Triggers deployment of an application's OPA policy script.
     *
     * @param userPrincipal the authenticated user
     * @param id            the application UUID
     * @param force         whether to bypass the pending deployment guard (default: false)
     * @return the updated application with HTTP 201 status
     */
    @PostMapping("/{id}/deploy")
    @Operation(summary = "Trigger deployment of an application's OPA policy")
    @ApiResponse(responseCode = "201", description = "Application deployment triggered/updated")
    @ApiResponse(responseCode = "404", description = "Application not found", content = @Content)
    public ResponseEntity<ApplicationDTO> deploy(
            @AuthenticationPrincipal final UserPrincipal userPrincipal,
            @PathVariable final UUID id,
            @RequestParam(name = "force", defaultValue = "false")
            @Schema(
                    defaultValue = "false",
                    description = "Force the deployment even if the application is already deployed."
            )
            final boolean force) {
        log.info("[{}] Received POST request to deploy application {} with force={}", userPrincipal.getEmail(), id,
                force);
        var application = opaApplicationDeployerService.deploy(userPrincipal, id, force);
        return ResponseEntity.status(HttpStatus.OK).body(applicationMapper.toDTO(application));
    }
}
