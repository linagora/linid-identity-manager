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

import io.github.linagora.linid.im.api.model.superset.SupersetTokenDTO;
import io.github.linagora.linid.im.api.model.superset.SupersetTokenRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.service.SupersetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller exposing Superset integration endpoints.
 * <p>
 * Provides operations for generating guest tokens used by embedded dashboards
 * and resolving dashboard identifiers from dashboard slugs.
 */
@Slf4j
@RestController
@RequestMapping("/superset")
@RequiredArgsConstructor
@Tag(name = "Superset", description = "Superset management endpoints")
public class SupersetController {

    /**
     * Service handling superset business logic.
     */
    private final SupersetService service;

    /**
     * Creates a Superset guest token for an embedded dashboard.
     *
     * @param userPrincipal the authenticated user performing the operation
     * @param tokenRecord   the guest token creation request
     * @return the generated guest token
     */
    @PostMapping("/token")
    @Operation(summary = "Create a Superset guest token")
    @ApiResponse(responseCode = "201", description = "Guest token successfully created")
    @ApiResponse(responseCode = "400", description = "Invalid request body")
    @ApiResponse(responseCode = "404", description = "Dashboard not found")
    public ResponseEntity<SupersetTokenDTO> create(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @Valid @RequestBody final SupersetTokenRecord tokenRecord) {
        log.info("[{}] Received POST request to create superset guest token with {}", userPrincipal.getEmail(),
            tokenRecord);
        var token = service.getToken(userPrincipal, tokenRecord);
        return ResponseEntity.status(HttpStatus.CREATED).body(token);
    }

    /**
     * Resolves a Superset dashboard identifier from its slug.
     *
     * @param userPrincipal the authenticated user performing the operation
     * @param slug          the Superset dashboard slug
     * @return the matching dashboard identifier
     */
    @GetMapping("/dashboard-id/{slug}")
    @Operation(summary = "Get a Superset dashboard ID by slug")
    @ApiResponse(responseCode = "200", description = "Dashboard ID found")
    @ApiResponse(responseCode = "404", description = "Dashboard not found")
    public ResponseEntity<UUID> getDashboardId(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final String slug) {
        log.info("[{}] Received GET request to get superset dashboard id from {}", userPrincipal.getEmail(), slug);
        var dashboardId = service.getDashboardId(slug);
        return ResponseEntity.ok(dashboardId);
    }
}
