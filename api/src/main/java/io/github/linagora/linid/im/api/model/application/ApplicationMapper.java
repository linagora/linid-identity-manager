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

package io.github.linagora.linid.im.api.model.application;

import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.Application;
import io.github.linagora.linid.im.api.persistence.model.ApplicationView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper converting an {@link ApplicationRecord} into an {@link Application} entity, and
 * {@link Application} / {@link ApplicationView} entities into their {@link ApplicationDTO} /
 * {@link ApplicationViewDTO} representations.
 */
@Mapper(componentModel = "spring")
public interface ApplicationMapper {

    /**
     * Creates a new {@link Application} entity from an {@link ApplicationRecord} and the calling principal.
     *
     * <p>Generated and externally-managed fields ({@code id}, {@code script}, {@code scriptChecksum},
     * {@code deployedAt}, {@code configuration}, {@code roles}, {@code insertDate}, {@code updateDate})
     * are left unset: {@code script}/{@code scriptChecksum}/{@code deployedAt}/{@code configuration} are
     * managed by a separate process, {@code roles} are managed through the dedicated roles endpoint. The
     * principal's identifier feeds {@code createdBy} and {@code updatedBy}.</p>
     *
     * @param record        the creation request record
     * @param userPrincipal the authenticated principal performing the creation
     * @return a partially populated {@link Application} entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "script", ignore = true)
    @Mapping(target = "scriptChecksum", ignore = true)
    @Mapping(target = "deployedAt", ignore = true)
    @Mapping(target = "configuration", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "insertDate", ignore = true)
    @Mapping(target = "updateDate", ignore = true)
    @Mapping(target = "createdBy", source = "userPrincipal.id")
    @Mapping(target = "updatedBy", source = "userPrincipal.id")
    Application toEntity(ApplicationRecord record, UserPrincipal userPrincipal);

    /**
     * Converts an {@link Application} entity to an {@link ApplicationDTO}.
     *
     * @param application the application entity
     * @return the corresponding DTO
     */
    ApplicationDTO toDTO(Application application);

    /**
     * Converts an {@link ApplicationView} entity to an {@link ApplicationViewDTO}.
     *
     * @param applicationView the application view entity
     * @return the corresponding DTO
     */
    ApplicationViewDTO toDTO(ApplicationView applicationView);

    /**
     * Converts an {@link ApplicationRoleRecord} request payload to an {@link ApplicationRoleDTO}.
     *
     * @param record the role request payload
     * @return the corresponding DTO
     */
    ApplicationRoleDTO toRoleDTO(ApplicationRoleRecord record);
}
