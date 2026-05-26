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

import io.github.linagora.linid.im.api.model.organizationalunit.OrganizationalUnitAccountMapper;
import io.github.linagora.linid.im.api.model.organizationalunit.OrganizationalUnitAccountViewDTO;
import io.github.linagora.linid.im.api.model.organizationalunit.OrganizationalUnitDTO;
import io.github.linagora.linid.im.api.model.organizationalunit.OrganizationalUnitMapper;
import io.github.linagora.linid.im.api.model.organizationalunit.OrganizationalUnitRecord;
import io.github.linagora.linid.im.api.model.organizationalunit.OrganizationalUnitStatusRecord;
import io.github.linagora.linid.im.api.model.organizationalunit.OrganizationalUnitViewDTO;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.OrganizationalUnitAccountViewQueryFilterDto;
import io.github.linagora.linid.im.api.persistence.model.OrganizationalUnitViewQueryFilterDto;
import io.github.linagora.linid.im.api.service.OrganizationalUnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

import java.util.List;
import java.util.UUID;

/**
 * REST controller exposing organizational unit management endpoints.
 * <p>Provides operations for creating, retrieving, updating, listing,
 * and deleting organizational units.
 */
@Slf4j
@RestController
@RequestMapping("/organizational-units")
@RequiredArgsConstructor
@Tag(name = "Organizational units", description = "Organizational units management endpoints")
public class OrganizationalUnitController {

    /**
     * Service handling organizational unit business logic.
     */
    private final OrganizationalUnitService service;

    /**
     * Mapper for entity-to-DTO conversion.
     */
    private final OrganizationalUnitMapper mapper;

    /**
     * Mapper for entity-to-DTO conversion.
     */
    private final OrganizationalUnitAccountMapper accountMapper;

    /**
     * Resolver for paginated response HTTP status.
     */
    private final PagedResponseStatusResolver pagedResponseStatusResolver;

    /**
     * Creates a new organizational unit.
     *
     * @param userPrincipal      the authenticated user performing the operation
     * @param organizationalUnit the organizational unit payload
     * @return the created organizational unit
     */
    @PostMapping
    @Operation(summary = "Create a new organizational unit")
    @ApiResponse(responseCode = "201", description = "Organizational unit successfully created")
    @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    public ResponseEntity<OrganizationalUnitDTO> create(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @Valid @RequestBody final OrganizationalUnitRecord organizationalUnit) {
        log.info("[{}] Received POST request to create organizational unit with {}", userPrincipal.getEmail(),
            organizationalUnit);

        var entity = service.create(userPrincipal, organizationalUnit);

        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toDTO(entity));
    }

    /**
     * Retrieves organizational units using pagination and optional filtering.
     *
     * @param userPrincipal the authenticated user performing the operation
     * @param filters       the filtering criteria
     * @param pageable      pagination information
     * @return a paginated list of organizational units
     */
    @GetMapping
    @Operation(summary = "Get all organizational units with pagination and filtering")
    @ApiResponse(responseCode = "200", description = "Full list of organizational units")
    @ApiResponse(responseCode = "206", description = "Partial list of organizational units (more pages available)")
    public ResponseEntity<Page<OrganizationalUnitViewDTO>> findAll(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        final OrganizationalUnitViewQueryFilterDto filters,
        final Pageable pageable) {
        log.info("[{}] Received GET request to list organizational units with filters {} and pageable {}",
            userPrincipal.getEmail(), filters, pageable);

        var page = service.findAll(userPrincipal, filters, pageable).map(mapper::toDTO);

        return pagedResponseStatusResolver.resolve(page);
    }

    /**
     * Retrieves an organizational unit by its identifier.
     *
     * @param userPrincipal the authenticated user performing the operation
     * @param id            the organizational unit identifier
     * @return the matching organizational unit
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get an organizational unit by ID")
    @ApiResponse(responseCode = "200", description = "Organizational unit found")
    @ApiResponse(responseCode = "404", description = "Organizational unit not found", content = @Content)
    public ResponseEntity<OrganizationalUnitViewDTO> findById(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID id) {
        log.info("[{}] Received GET request for organizational unit {}", userPrincipal.getEmail(), id);

        var entity = service.findViewById(userPrincipal, id);

        return ResponseEntity.ok(mapper.toDTO(entity));
    }

    /**
     * Retrieves organizational unit accounts using pagination and optional filtering.
     *
     * @param userPrincipal        the authenticated user performing the operation
     * @param organizationalUnitId the organizational unit identifier
     * @param filters              the filtering criteria
     * @param pageable             pagination information
     * @return the matching organizational unit
     */
    @GetMapping("/{organizationalUnitId}/accounts")
    @Operation(summary = "Get all organizational unit accounts with pagination and filtering")
    @ApiResponse(responseCode = "200", description = "Full list of organizational unit accounts")
    @ApiResponse(responseCode = "206",
        description = "Partial list of organizational unit accounts (more pages available)")
    public ResponseEntity<Page<OrganizationalUnitAccountViewDTO>> findAllAccounts(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID organizationalUnitId,
        final OrganizationalUnitAccountViewQueryFilterDto filters,
        final Pageable pageable) {
        log.info("[{}] Received GET request for organizational unit {} accounts with {}", userPrincipal.getEmail(),
            organizationalUnitId, filters);

        service.existsById(userPrincipal, organizationalUnitId);

        filters.setOrganizationalUnitId(List.of(organizationalUnitId.toString()));

        var pages = service.findAllAccounts(userPrincipal, filters, pageable)
            .map(accountMapper::toDTO);

        return pagedResponseStatusResolver.resolve(pages);
    }

    /**
     * Updates an existing organizational unit.
     *
     * @param userPrincipal the authenticated user performing the operation
     * @param id            the organizational unit identifier
     * @param record        the updated organizational unit payload
     * @return the updated organizational unit
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update the organizational unit")
    @ApiResponse(responseCode = "200", description = "Organizational unit successfully updated")
    @ApiResponse(responseCode = "404", description = "Organizational unit not found", content = @Content)
    public ResponseEntity<OrganizationalUnitDTO> update(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID id,
        @Valid @RequestBody final OrganizationalUnitRecord record) {
        log.info("[{}] Received PUT request for organizational unit {} with {}", userPrincipal.getEmail(), id, record);
        var entity = service.update(userPrincipal, id, record);
        return ResponseEntity.ok(mapper.toDTO(entity));
    }

    /**
     * Updates the suspension status of an organizational unit (upsert behaviour).
     *
     * <p>This is the single entry point for managing suspension. A {@code null}
     * {@code suspensionPeriod} removes the suspension.</p>
     *
     * @param userPrincipal the authenticated user performing the operation
     * @param id            the organizational unit identifier
     * @param record        the requested suspension status fields
     * @return the updated organizational unit including its embedded status
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "Update the suspension status of an organizational unit")
    @ApiResponse(responseCode = "200", description = "Status successfully updated")
    @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    @ApiResponse(responseCode = "404", description = "Organizational unit not found", content = @Content)
    public ResponseEntity<OrganizationalUnitViewDTO> updateStatus(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID id,
        @Valid @RequestBody final OrganizationalUnitStatusRecord record) {
        log.info("[{}] Received PUT request for organizational unit {} to update status with {}",
            userPrincipal.getEmail(), id, record);
        var view = service.updateStatus(userPrincipal, id, record);
        return ResponseEntity.ok(mapper.toDTO(view));
    }

    /**
     * Deletes an organizational unit by its identifier.
     *
     * @param userPrincipal the authenticated user performing the operation
     * @param id            the organizational unit identifier
     * @return an empty response with HTTP 204 status
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an organizational unit by ID")
    @ApiResponse(responseCode = "204", description = "Organizational unit successfully deleted")
    @ApiResponse(responseCode = "404", description = "Organizational unit not found", content = @Content)
    public ResponseEntity<Void> deleteById(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID id) {
        log.info("[{}] Received DELETE request for organizational unit {}", userPrincipal.getEmail(), id);
        service.deleteById(userPrincipal, id);
        return ResponseEntity.noContent().build();
    }
}
