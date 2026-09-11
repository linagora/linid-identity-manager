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

import io.github.linagora.linid.im.api.model.group.GroupRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.Group;
import io.github.linagora.linid.im.api.persistence.model.GroupView;
import io.github.linagora.linid.im.api.persistence.model.GroupViewQueryFilterDto;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for group management operations.
 */
public interface GroupService {

    /**
     * Creates a new group from the given request.
     *
     * <p>The {@code code} must be unique. When provided, the parent group, the organizational unit and the
     * application must exist.</p>
     *
     * @param userPrincipal the authenticated user
     * @param group         the group creation record
     * @return the created group entity
     */
    Group create(UserPrincipal userPrincipal, GroupRecord group);

    /**
     * Retrieves a paginated list of groups, optionally filtered.
     *
     * @param userPrincipal the authenticated user
     * @param filters       generated filter DTO from query parameters
     * @param pageable      pagination information
     * @return a page of group views
     */
    Page<GroupView> findAll(UserPrincipal userPrincipal, GroupViewQueryFilterDto filters, Pageable pageable);

    /**
     * Retrieves a group entity by its unique identifier.
     *
     * @param userPrincipal the authenticated user
     * @param id            the group UUID
     * @return the group entity
     */
    Group findById(UserPrincipal userPrincipal, UUID id);

    /**
     * Retrieves a group view by its unique identifier.
     *
     * @param userPrincipal the authenticated user
     * @param id            the group UUID
     * @return the group view entity
     */
    GroupView findViewById(UserPrincipal userPrincipal, UUID id);

    /**
     * Updates the group with the given identifier.
     *
     * <p>The {@code code} must remain unique. When provided, the parent group, the organizational unit and the
     * application must exist, and the parent group must not create a cycle in the group hierarchy.</p>
     *
     * @param userPrincipal the authenticated user
     * @param id            the group UUID
     * @param group         the update record
     * @return the updated group entity
     */
    Group update(UserPrincipal userPrincipal, UUID id, GroupRecord group);

    /**
     * Deletes a group by its unique identifier.
     *
     * @param userPrincipal the authenticated user
     * @param id            the group UUID
     */
    void deleteById(UserPrincipal userPrincipal, UUID id);
}
