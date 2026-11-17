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

import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.GlobalRole;
import io.github.linagora.linid.im.api.persistence.model.GlobalRoleView;
import io.github.linagora.linid.im.api.persistence.model.GlobalRoleViewQueryFilterDto;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for global role management operations.
 */
public interface GlobalRoleService {

    /**
     * Assigns a global role to an account.
     *
     * <p>The role must be one of the stored global roles, the account must exist and must not already
     * hold the role.</p>
     *
     * @param userPrincipal the authenticated user
     * @param accountId     the UUID of the account receiving the role
     * @param role          the global role to assign
     * @return the created {@link GlobalRole}
     */
    GlobalRole create(UserPrincipal userPrincipal, UUID accountId, String role);

    /**
     * Retrieves a paginated list of global roles, optionally filtered.
     *
     * @param userPrincipal the authenticated user
     * @param filters       generated filter DTO from query parameters
     * @param pageable      pagination information
     * @return a page of {@link GlobalRoleView}
     */
    Page<GlobalRoleView> findAll(UserPrincipal userPrincipal, GlobalRoleViewQueryFilterDto filters, Pageable pageable);

    /**
     * Retrieves a paginated list of the global roles held by an account, optionally filtered.
     *
     * @param userPrincipal the authenticated user
     * @param accountId     the account UUID
     * @param filters       generated filter DTO from query parameters, restricted to the account
     * @param pageable      pagination information
     * @return a page of {@link GlobalRoleView}
     */
    Page<GlobalRoleView> findAllByAccountId(UserPrincipal userPrincipal,
                                            UUID accountId,
                                            GlobalRoleViewQueryFilterDto filters,
                                            Pageable pageable);
}
