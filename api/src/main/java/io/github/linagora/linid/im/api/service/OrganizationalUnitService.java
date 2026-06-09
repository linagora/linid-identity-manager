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

package io.github.linagora.linid.im.api.service;

import io.github.linagora.linid.im.api.model.organizationalunit.OrganizationalUnitRecord;
import io.github.linagora.linid.im.api.model.organizationalunit.OrganizationalUnitReactivationRecord;
import io.github.linagora.linid.im.api.model.organizationalunit.OrganizationalUnitSuspensionRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.OrganizationalUnit;
import io.github.linagora.linid.im.api.persistence.model.OrganizationalUnitAccountView;
import io.github.linagora.linid.im.api.persistence.model.OrganizationalUnitAccountViewQueryFilterDto;
import io.github.linagora.linid.im.api.persistence.model.OrganizationalUnitView;
import io.github.linagora.linid.im.api.persistence.model.OrganizationalUnitViewQueryFilterDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface responsible for managing {@link OrganizationalUnit} domain operations.
 * <p>This service provides CRUD capabilities for organizational units, along with
 * filtered and paginated search support. All operations are executed in the context
 * of an authenticated user represented by {@link UserPrincipal}, enabling
 * authorization, auditing, and business-rule enforcement.
 */
public interface OrganizationalUnitService {
    /**
     * Creates a new organizational unit.
     *
     * @param userPrincipal      the authenticated user performing the operation
     * @param organizationalUnit the data required to create the organizational unit
     * @return the created {@link OrganizationalUnit}
     */
    OrganizationalUnit create(UserPrincipal userPrincipal, OrganizationalUnitRecord organizationalUnit);

    /**
     * Retrieves an organizational unit by its unique identifier.
     *
     * @param userPrincipal the authenticated user performing the operation
     * @param id            the unique identifier of the organizational unit
     * @return the found {@link OrganizationalUnit}
     */
    OrganizationalUnit findById(UserPrincipal userPrincipal, UUID id);

    /**
     * Checks whether an organizational unit exists for the given identifier.
     *
     * @param userPrincipal the authenticated user performing the operation
     * @param id            the unique identifier of the organizational unit
     */
    void existsById(UserPrincipal userPrincipal, UUID id);

    /**
     * Retrieves an organizational unit view by its unique identifier.
     *
     * @param userPrincipal the authenticated user performing the operation
     * @param id            the unique identifier of the organizational unit
     * @return the found {@link OrganizationalUnitView}
     */
    OrganizationalUnitView findViewById(UserPrincipal userPrincipal, UUID id);

    /**
     * Retrieves all organizational units view matching the provided filters in a paginated format.
     *
     * @param userPrincipal the authenticated user performing the operation
     * @param filters       filtering criteria applied to the search
     * @param pageable      pagination and sorting information
     * @return a page of {@link OrganizationalUnitView}
     */
    Page<OrganizationalUnitView> findAll(
        UserPrincipal userPrincipal,
        OrganizationalUnitViewQueryFilterDto filters,
        Pageable pageable
    );

    /**
     * Retrieves a paginated list of organizational unit account views matching the provided filters.
     *
     * @param userPrincipal the authenticated user performing the request
     * @param filters       filtering criteria applied to the query
     * @param pageable      pagination and sorting parameters
     * @return a page of {@link OrganizationalUnitAccountView}
     */
    Page<OrganizationalUnitAccountView> findAllAccounts(
        UserPrincipal userPrincipal,
        OrganizationalUnitAccountViewQueryFilterDto filters,
        Pageable pageable
    );

    /**
     * Deletes an organizational unit by its unique identifier.
     *
     * <p>Business constraints may prevent deletion of system-reserved units
     * such as the ROOT organizational unit.
     *
     * @param userPrincipal the authenticated user performing the operation
     * @param id            the unique identifier of the organizational unit to delete
     */
    void deleteById(UserPrincipal userPrincipal, UUID id);

    /**
     * Updates an existing organizational unit.
     *
     * @param userPrincipal the authenticated user performing the operation
     * @param id            the unique identifier of the organizational unit to update
     * @param record        the updated data for the organizational unit
     * @return the updated {@link OrganizationalUnit}
     */
    OrganizationalUnit update(
        UserPrincipal userPrincipal,
        UUID id,
        OrganizationalUnitRecord record
    );

    /**
     * Suspends an organizational unit, either immediately or as a scheduled suspension, after
     * enforcing the business rules delegated to {@code OrganizationalUnitSuspensionValidator}.
     *
     * @param userPrincipal the authenticated user performing the operation
     * @param id            the unique identifier of the organizational unit
     * @param record        the suspension request (suspension period and reason fields)
     * @return the updated {@link OrganizationalUnitView} including the embedded status
     */
    OrganizationalUnitView suspend(
        UserPrincipal userPrincipal,
        UUID id,
        OrganizationalUnitSuspensionRecord record
    );

    /**
     * Reactivates an organizational unit (lifts its suspension), after enforcing the business rules
     * delegated to {@code OrganizationalUnitReactivationValidator}.
     *
     * @param userPrincipal the authenticated user performing the operation
     * @param id            the unique identifier of the organizational unit
     * @param record        the reactivation request (mandatory justification comment)
     * @return the updated {@link OrganizationalUnitView} including the embedded status
     */
    OrganizationalUnitView reactivate(
        UserPrincipal userPrincipal,
        UUID id,
        OrganizationalUnitReactivationRecord record
    );
}
