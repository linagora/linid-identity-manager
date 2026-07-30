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

import io.github.linagora.linid.im.api.model.application.role.ApplicationRoleRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.ApplicationRole;
import io.github.linagora.linid.im.api.persistence.model.ApplicationRoleView;
import io.github.linagora.linid.im.api.persistence.model.ApplicationRoleViewQueryFilterDto;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing the roles exposed by an application.
 *
 * <p>All operations are scoped to the owning application, which must exist.</p>
 */
public interface ApplicationRoleService {

    /**
     * Creates a new role for the given application.
     *
     * @param userPrincipal the authenticated user
     * @param applicationId the identifier of the owning application
     * @param role          the role creation record
     * @return the created application role
     */
    ApplicationRole create(UserPrincipal userPrincipal, UUID applicationId, ApplicationRoleRecord role);

    /**
     * Retrieves a paginated and optionally filtered list of roles of the given application.
     *
     * @param userPrincipal the authenticated user
     * @param applicationId the identifier of the owning application
     * @param filters       the query filters applied to the view
     * @param pageable      the pagination parameters
     * @return a page of application role views
     */
    Page<ApplicationRoleView> findAll(
        UserPrincipal userPrincipal,
        UUID applicationId,
        ApplicationRoleViewQueryFilterDto filters,
        Pageable pageable);

    /**
     * Retrieves a role of the given application by its identifier.
     *
     * @param userPrincipal the authenticated user
     * @param applicationId the identifier of the owning application
     * @param id            the role identifier
     * @return the matching application role
     */
    ApplicationRole findById(UserPrincipal userPrincipal, UUID applicationId, UUID id);

    /**
     * Retrieves the enriched view of a role of the given application by its identifier.
     *
     * @param userPrincipal the authenticated user
     * @param applicationId the identifier of the owning application
     * @param id            the role identifier
     * @return the matching application role view
     */
    ApplicationRoleView findViewById(UserPrincipal userPrincipal, UUID applicationId, UUID id);

    /**
     * Updates a role of the given application.
     *
     * @param userPrincipal the authenticated user
     * @param applicationId the identifier of the owning application
     * @param id            the role identifier
     * @param role          the role update record
     * @return the updated application role
     */
    ApplicationRole update(UserPrincipal userPrincipal, UUID applicationId, UUID id, ApplicationRoleRecord role);

    /**
     * Deletes a role of the given application by its identifier.
     *
     * @param userPrincipal the authenticated user
     * @param applicationId the identifier of the owning application
     * @param id            the role identifier
     */
    void deleteById(UserPrincipal userPrincipal, UUID applicationId, UUID id);
}
