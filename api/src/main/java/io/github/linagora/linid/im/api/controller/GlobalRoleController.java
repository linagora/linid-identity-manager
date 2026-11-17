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

import io.github.linagora.linid.im.api.model.globalrole.GlobalRoleDTO;
import io.github.linagora.linid.im.api.model.globalrole.GlobalRoleMapper;
import io.github.linagora.linid.im.api.model.globalrole.GlobalRoleViewDTO;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.GlobalRoleEnum;
import io.github.linagora.linid.im.api.persistence.model.GlobalRoleViewQueryFilterDto;
import io.github.linagora.linid.im.api.service.GlobalRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for global role management endpoints.
 *
 * <p>Provides the listing, assignment and removal of global roles, with pagination and filtering
 * support via {@code spring-query-filter}.</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Global roles", description = "Global role management endpoints")
public class GlobalRoleController {

    /**
     * Service handling global role business logic.
     */
    private final GlobalRoleService globalRoleService;

    /**
     * Mapper for entity-to-DTO conversion.
     */
    private final GlobalRoleMapper globalRoleMapper;

    /**
     * Resolver for paginated response HTTP status.
     */
    private final PagedResponseStatusResolver pagedResponseStatusResolver;

    /**
     * Retrieves a paginated and optionally filtered list of global roles.
     *
     * @param userPrincipal the authenticated user
     * @param filters       generated filter DTO from query parameters
     * @param pageable      pagination parameters
     * @return a page of global role view DTOs
     */
    @GetMapping("/global-roles")
    @Operation(summary = "Get all global roles with pagination and filtering")
    @ApiResponse(responseCode = "200", description = "Full list of global roles")
    @ApiResponse(responseCode = "206", description = "Partial list of global roles (more pages available)")
    public ResponseEntity<Page<GlobalRoleViewDTO>> findAll(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        final GlobalRoleViewQueryFilterDto filters,
        final Pageable pageable) {
        log.info("[{}] Received GET request to list global roles with filters {} and pageable {}",
            userPrincipal.getEmail(), filters, pageable);
        var page = globalRoleService.findAll(userPrincipal, filters, pageable).map(globalRoleMapper::toDTO);
        return pagedResponseStatusResolver.resolve(page);
    }

    /**
     * Retrieves the global roles held by an account, with pagination and filtering.
     *
     * @param userPrincipal the authenticated user
     * @param accountId     the account UUID
     * @param filters       generated filter DTO from query parameters
     * @param pageable      pagination parameters
     * @return a page of global role view DTOs
     */
    @GetMapping("/accounts/{accountId}/global-roles")
    @Operation(summary = "Get all global roles of an account with pagination and filtering")
    @ApiResponse(responseCode = "200", description = "Full list of the account global roles")
    @ApiResponse(responseCode = "206",
        description = "Partial list of the account global roles (more pages available)")
    @ApiResponse(responseCode = "404", description = "Account not found", content = @Content)
    public ResponseEntity<Page<GlobalRoleViewDTO>> findAllByAccountId(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID accountId,
        final GlobalRoleViewQueryFilterDto filters,
        final Pageable pageable) {
        log.info("[{}] Received GET request for account {} global roles with filters {} and pageable {}",
            userPrincipal.getEmail(), accountId, filters, pageable);
        var page = globalRoleService.findAllByAccountId(userPrincipal, accountId, filters, pageable)
            .map(globalRoleMapper::toDTO);
        return pagedResponseStatusResolver.resolve(page);
    }

    /**
     * Assigns a global role to an account.
     *
     * @param userPrincipal the authenticated user
     * @param accountId     the UUID of the account receiving the role
     * @param role          the global role to assign
     * @return the created global role with HTTP 201 status
     */
    @PostMapping("/accounts/{accountId}/global-roles/{role}")
    @Operation(summary = "Assign a global role to an account")
    @ApiResponse(responseCode = "201", description = "Global role successfully assigned")
    @ApiResponse(responseCode = "400",
        description = "Unknown role (SUPER_ADMIN included) or role already assigned to the account",
        content = @Content)
    @ApiResponse(responseCode = "404", description = "Account not found", content = @Content)
    public ResponseEntity<GlobalRoleDTO> create(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID accountId,
        @Parameter(schema = @Schema(implementation = GlobalRoleEnum.class)) @PathVariable final String role) {
        log.info("[{}] Received POST request to assign global role {} to account {}", userPrincipal.getEmail(),
            role, accountId);
        var entity = globalRoleService.create(userPrincipal, accountId, role);
        return ResponseEntity.status(HttpStatus.CREATED).body(globalRoleMapper.toDTO(entity));
    }
}
