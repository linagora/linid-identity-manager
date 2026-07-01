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

import io.github.linagora.linid.im.api.model.application.rule.ApplicationRuleDTO;
import io.github.linagora.linid.im.api.model.application.rule.ApplicationRuleMapper;
import io.github.linagora.linid.im.api.model.application.rule.ApplicationRuleRecord;
import io.github.linagora.linid.im.api.model.application.rule.ApplicationRuleViewDTO;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.ApplicationRuleViewQueryFilterDto;
import io.github.linagora.linid.im.api.service.ApplicationRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

import java.util.UUID;

/**
 * REST controller for application rule management endpoints.
 *
 * <p>Provides CRUD operations for the rules of a given application, with pagination and filtering
 * support via {@code spring-query-filter}. All endpoints are nested under the owning application.</p>
 */
@Slf4j
@RestController
@RequestMapping("/applications/{applicationId}/rules")
@RequiredArgsConstructor
@Tag(name = "Application Rules", description = "Application rule management endpoints")
public class ApplicationRuleController {

    /**
     * Service handling application rule business logic.
     */
    private final ApplicationRuleService applicationRuleService;

    /**
     * Mapper for entity-to-DTO conversion.
     */
    private final ApplicationRuleMapper applicationRuleMapper;

    /**
     * Resolver for paginated response HTTP status.
     */
    private final PagedResponseStatusResolver pagedResponseStatusResolver;

    /**
     * Creates a new rule for the given application.
     *
     * @param userPrincipal the authenticated user
     * @param applicationId the owning application UUID
     * @param rule          the rule creation record with validated fields
     * @return the created rule with HTTP 201 status
     */
    @PostMapping
    @Operation(summary = "Create a new application rule")
    @ApiResponse(responseCode = "201", description = "Application rule successfully created")
    @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    @ApiResponse(responseCode = "404", description = "Application not found", content = @Content)
    public ResponseEntity<ApplicationRuleDTO> create(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID applicationId,
        @Valid @RequestBody final ApplicationRuleRecord rule) {
        log.info("[{}] Received POST request to create rule for application {} with {}",
            userPrincipal.getEmail(), applicationId, rule);
        var entity = applicationRuleService.create(userPrincipal, applicationId, rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationRuleMapper.toDTO(entity));
    }

    /**
     * Retrieves a paginated and optionally filtered list of rules for the given application.
     *
     * <p>Rules are sorted by {@code priority} ascending by default.</p>
     *
     * @param userPrincipal the authenticated user
     * @param applicationId the owning application UUID
     * @param filters       generated filter DTO from query parameters
     * @param pageable      pagination parameters
     * @return a page of application rule view DTOs
     */
    @GetMapping
    @Operation(summary = "Get all rules of an application with pagination and filtering")
    @ApiResponse(responseCode = "200", description = "Full list of application rules")
    @ApiResponse(responseCode = "206", description = "Partial list of application rules (more pages available)")
    @ApiResponse(responseCode = "404", description = "Application not found", content = @Content)
    public ResponseEntity<Page<ApplicationRuleViewDTO>> findAll(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID applicationId,
        final ApplicationRuleViewQueryFilterDto filters,
        @PageableDefault(sort = "priority", direction = Sort.Direction.ASC) final Pageable pageable) {
        log.info("[{}] Received GET request to list rules of application {} with filters {} and pageable {}",
            userPrincipal.getEmail(), applicationId, filters, pageable);
        var page = applicationRuleService.findAll(userPrincipal, applicationId, filters, pageable)
            .map(applicationRuleMapper::toDTO);
        return pagedResponseStatusResolver.resolve(page);
    }

    /**
     * Retrieves a rule of the given application by its unique identifier.
     *
     * @param userPrincipal the authenticated user
     * @param applicationId the owning application UUID
     * @param ruleId        the rule UUID
     * @return the application rule view DTO
     */
    @GetMapping("/{ruleId}")
    @Operation(summary = "Get an application rule by ID")
    @ApiResponse(responseCode = "200", description = "Application rule found")
    @ApiResponse(responseCode = "404", description = "Application rule not found", content = @Content)
    public ResponseEntity<ApplicationRuleViewDTO> findById(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID applicationId,
        @PathVariable final UUID ruleId) {
        log.info("[{}] Received GET request for rule {} of application {}",
            userPrincipal.getEmail(), ruleId, applicationId);
        var entity = applicationRuleService.findViewById(userPrincipal, applicationId, ruleId);
        return ResponseEntity.ok(applicationRuleMapper.toDTO(entity));
    }

    /**
     * Updates a rule of the given application.
     *
     * @param userPrincipal the authenticated user
     * @param applicationId the owning application UUID
     * @param ruleId        the rule UUID
     * @param rule          the update record with validated fields
     * @return the updated rule
     */
    @PutMapping("/{ruleId}")
    @Operation(summary = "Update an application rule")
    @ApiResponse(responseCode = "200", description = "Application rule successfully updated")
    @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    @ApiResponse(responseCode = "404", description = "Application rule not found", content = @Content)
    public ResponseEntity<ApplicationRuleDTO> update(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID applicationId,
        @PathVariable final UUID ruleId,
        @Valid @RequestBody final ApplicationRuleRecord rule) {
        log.info("[{}] Received PUT request to update rule {} of application {} with {}",
            userPrincipal.getEmail(), ruleId, applicationId, rule);
        var entity = applicationRuleService.update(userPrincipal, applicationId, ruleId, rule);
        return ResponseEntity.ok(applicationRuleMapper.toDTO(entity));
    }

    /**
     * Deletes a rule of the given application by its unique identifier.
     *
     * @param userPrincipal the authenticated user
     * @param applicationId the owning application UUID
     * @param ruleId        the rule UUID
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{ruleId}")
    @Operation(summary = "Delete an application rule by ID")
    @ApiResponse(responseCode = "204", description = "Application rule successfully deleted")
    @ApiResponse(responseCode = "404", description = "Application rule not found", content = @Content)
    public ResponseEntity<Void> deleteById(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID applicationId,
        @PathVariable final UUID ruleId) {
        log.info("[{}] Received DELETE request for rule {} of application {}",
            userPrincipal.getEmail(), ruleId, applicationId);
        applicationRuleService.deleteById(userPrincipal, applicationId, ruleId);
        return ResponseEntity.noContent().build();
    }
}
