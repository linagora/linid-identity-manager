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

import io.github.linagora.linid.im.api.model.account.AccountActivationRecord;
import io.github.linagora.linid.im.api.model.account.AccountDTO;
import io.github.linagora.linid.im.api.model.account.AccountDeactivationRecord;
import io.github.linagora.linid.im.api.model.account.AccountMapper;
import io.github.linagora.linid.im.api.model.account.AccountReactivationRecord;
import io.github.linagora.linid.im.api.model.account.AccountRecord;
import io.github.linagora.linid.im.api.model.account.AccountSuspensionRecord;
import io.github.linagora.linid.im.api.model.account.AccountValidityRecord;
import io.github.linagora.linid.im.api.model.account.AccountViewDTO;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.AccountViewQueryFilterDto;
import io.github.linagora.linid.im.api.service.AccountService;
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

import java.util.UUID;

/**
 * REST controller for account management endpoints.
 *
 * <p>Provides CRUD operations for accounts with pagination and filtering
 * support via {@code spring-query-filter}.</p>
 */
@Slf4j
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Account management endpoints")
public class AccountController {

    /**
     * Service handling account business logic.
     */
    private final AccountService accountService;

    /**
     * Mapper for entity-to-DTO conversion.
     */
    private final AccountMapper accountMapper;

    /**
     * Resolver for paginated response HTTP status.
     */
    private final PagedResponseStatusResolver pagedResponseStatusResolver;

    /**
     * Creates a new account.
     *
     * @param userPrincipal the authenticated user
     * @param account       the account creation record with validated fields
     * @return the created account with HTTP 201 status
     */
    @PostMapping
    @Operation(summary = "Create a new account")
    @ApiResponse(responseCode = "201", description = "Account successfully created")
    @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    public ResponseEntity<AccountDTO> create(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @Valid @RequestBody final AccountRecord account) {
        log.info("[{}] Received POST request to create account with {}", userPrincipal.getEmail(),
            account);
        var entity = accountService.create(userPrincipal, account);
        return ResponseEntity.status(HttpStatus.CREATED).body(accountMapper.toDTO(entity));
    }

    /**
     * Retrieves a paginated and optionally filtered list of accounts.
     *
     * @param userPrincipal the authenticated user
     * @param filters       generated filter DTO from query parameters
     * @param pageable      pagination parameters
     * @return a page of account DTOs
     */
    @GetMapping
    @Operation(summary = "Get all accounts with pagination and filtering")
    @ApiResponse(responseCode = "200", description = "Full list of accounts")
    @ApiResponse(responseCode = "206", description = "Partial list of accounts (more pages available)")
    public ResponseEntity<Page<AccountViewDTO>> findAll(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        final AccountViewQueryFilterDto filters,
        final Pageable pageable) {
        log.info("[{}] Received GET request to list accounts with filters {} and pageable {}",
            userPrincipal.getEmail(), filters, pageable);
        var page = accountService.findAll(userPrincipal, filters, pageable).map(accountMapper::toDTO);
        return pagedResponseStatusResolver.resolve(page);
    }

    /**
     * Retrieves an account by its unique identifier.
     *
     * @param userPrincipal the authenticated user
     * @param id            the account UUID
     * @return the account DTO
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get an account by ID")
    @ApiResponse(responseCode = "200", description = "Account found")
    @ApiResponse(responseCode = "404", description = "Account not found", content = @Content)
    public ResponseEntity<AccountViewDTO> findById(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID id) {
        log.info("[{}] Received GET request for account {}", userPrincipal.getEmail(), id);
        var entity = accountService.findById(userPrincipal, id);
        return ResponseEntity.ok(accountMapper.toDTO(entity));
    }

    /**
     * Deletes an account by its unique identifier.
     *
     * @param userPrincipal the authenticated user
     * @param id            the account UUID
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an account by ID")
    @ApiResponse(responseCode = "204", description = "Account successfully deleted")
    @ApiResponse(responseCode = "404", description = "Account not found", content = @Content)
    public ResponseEntity<Void> deleteById(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID id) {
        log.info("[{}] Received DELETE request for account {}", userPrincipal.getEmail(), id);
        accountService.deleteById(userPrincipal, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Suspends the account (immediate or scheduled suspension).
     *
     * @param userPrincipal the authenticated user
     * @param id            the account UUID
     * @param record        the suspension request (suspension period and reason fields)
     * @return the refreshed account view with computed {@code status} and {@code daysBeforeDeactivation}
     */
    @PutMapping("/{id}/status/suspend")
    @Operation(summary = "Suspend the account (immediate or scheduled)")
    @ApiResponse(responseCode = "200", description = "Account successfully suspended")
    @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    @ApiResponse(responseCode = "404", description = "Account not found", content = @Content)
    public ResponseEntity<AccountViewDTO> suspend(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID id,
        @Valid @RequestBody final AccountSuspensionRecord record) {
        log.info("[{}] Received PUT request for account {} to suspend with {}",
            userPrincipal.getEmail(), id, record);
        var view = accountService.suspend(userPrincipal, id, record);
        return ResponseEntity.ok(accountMapper.toDTO(view));
    }

    /**
     * Deactivates the account (immediate or scheduled deactivation, sets its validity period end).
     *
     * @param userPrincipal the authenticated user
     * @param id            the account UUID
     * @param record        the deactivation request (deactivation timestamp and reason fields)
     * @return the refreshed account view with computed {@code status} and {@code daysBeforeDeactivation}
     */
    @PutMapping("/{id}/status/deactivate")
    @Operation(summary = "Deactivate the account (immediate or scheduled)")
    @ApiResponse(responseCode = "200", description = "Account successfully deactivated")
    @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    @ApiResponse(responseCode = "404", description = "Account not found", content = @Content)
    public ResponseEntity<AccountViewDTO> deactivate(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID id,
        @Valid @RequestBody final AccountDeactivationRecord record) {
        log.info("[{}] Received PUT request for account {} to deactivate with {}",
            userPrincipal.getEmail(), id, record);
        var view = accountService.deactivate(userPrincipal, id, record);
        return ResponseEntity.ok(accountMapper.toDTO(view));
    }

    /**
     * Reactivates the account (lifts its suspension).
     *
     * @param userPrincipal the authenticated user
     * @param id            the account UUID
     * @param record        the reactivation request (mandatory justification comment)
     * @return the refreshed account view with computed {@code status} and {@code daysBeforeDeactivation}
     */
    @PutMapping("/{id}/status/reactivate")
    @Operation(summary = "Reactivate the account (lifts its suspension)")
    @ApiResponse(responseCode = "200", description = "Account successfully reactivated")
    @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    @ApiResponse(responseCode = "404", description = "Account not found", content = @Content)
    public ResponseEntity<AccountViewDTO> reactivate(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID id,
        @Valid @RequestBody final AccountReactivationRecord record) {
        log.info("[{}] Received PUT request for account {} to reactivate with {}",
            userPrincipal.getEmail(), id, record);
        var view = accountService.reactivate(userPrincipal, id, record);
        return ResponseEntity.ok(accountMapper.toDTO(view));
    }

    /**
     * Activates the account once business rules are satisfied.
     *
     * @param userPrincipal the authenticated user
     * @param id            the account UUID
     * @param record        the activation request carrying the new {@code activationAt}
     * @return the refreshed account view
     */
    @PutMapping("/{id}/status/activate")
    @Operation(summary = "Activate the account when business rules are met")
    @ApiResponse(responseCode = "200", description = "Account successfully activated")
    @ApiResponse(responseCode = "400", description = "Activation rules violated", content = @Content)
    @ApiResponse(responseCode = "404", description = "Account or account status not found", content = @Content)
    public ResponseEntity<AccountViewDTO> activate(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID id,
        @Valid @RequestBody final AccountActivationRecord record) {
        log.info("[{}] Received PUT request for account {} to update activation with {}",
            userPrincipal.getEmail(), id, record);
        var view = accountService.updateActivation(userPrincipal, id, record);
        return ResponseEntity.ok(accountMapper.toDTO(view));
    }

    /**
     * Schedules the validity period start of the account (administrative action, distinct from the
     * {@code activationAt} timestamp set by {@code /status/activate}).
     *
     * @param userPrincipal the authenticated user
     * @param id            the account UUID
     * @param record        the validity request (validity period start)
     * @return the refreshed account view with computed {@code status} and {@code daysBeforeDeactivation}
     */
    @PutMapping("/{id}/status/schedule-activation")
    @Operation(summary = "Schedule the validity period start of the account")
    @ApiResponse(responseCode = "200", description = "Validity successfully updated")
    @ApiResponse(responseCode = "400", description = "Invalid request body", content = @Content)
    @ApiResponse(responseCode = "404", description = "Account not found", content = @Content)
    public ResponseEntity<AccountViewDTO> scheduleActivation(
        @AuthenticationPrincipal final UserPrincipal userPrincipal,
        @PathVariable final UUID id,
        @Valid @RequestBody final AccountValidityRecord record) {
        log.info("[{}] Received PUT request for account {} to schedule activation with {}",
            userPrincipal.getEmail(), id, record);
        var view = accountService.updateValidity(userPrincipal, id, record);
        return ResponseEntity.ok(accountMapper.toDTO(view));
    }
}
