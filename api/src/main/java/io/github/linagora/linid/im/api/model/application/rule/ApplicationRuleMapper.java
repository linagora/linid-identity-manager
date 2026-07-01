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

package io.github.linagora.linid.im.api.model.application.rule;

import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.ApplicationRule;
import io.github.linagora.linid.im.api.persistence.model.ApplicationRuleView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper converting an {@link ApplicationRuleRecord} into an {@link ApplicationRule}
 * entity, and {@link ApplicationRule} / {@link ApplicationRuleView} entities into their
 * {@link ApplicationRuleDTO} / {@link ApplicationRuleViewDTO} representations.
 */
@Mapper(componentModel = "spring")
public interface ApplicationRuleMapper {

    /**
     * Creates a new {@link ApplicationRule} entity from an {@link ApplicationRuleRecord} and the
     * calling principal.
     *
     * <p>Generated and service-managed fields ({@code id}, {@code applicationId}, {@code scriptChecksum},
     * {@code disabled}, {@code insertDate}, {@code updateDate}) are left unset: {@code applicationId} is
     * derived from the request path, {@code scriptChecksum} is computed from the script, and
     * {@code disabled} is forced to {@code true} on creation. The principal's identifier feeds
     * {@code createdBy} and {@code updatedBy}.</p>
     *
     * @param record        the creation request record
     * @param userPrincipal the authenticated principal performing the creation
     * @return a partially populated {@link ApplicationRule} entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "applicationId", ignore = true)
    @Mapping(target = "scriptChecksum", ignore = true)
    @Mapping(target = "disabled", ignore = true)
    @Mapping(target = "insertDate", ignore = true)
    @Mapping(target = "updateDate", ignore = true)
    @Mapping(target = "createdBy", source = "userPrincipal.id")
    @Mapping(target = "updatedBy", source = "userPrincipal.id")
    ApplicationRule toEntity(ApplicationRuleRecord record, UserPrincipal userPrincipal);

    /**
     * Converts an {@link ApplicationRule} entity to an {@link ApplicationRuleDTO}.
     *
     * @param applicationRule the application rule entity
     * @return the corresponding DTO
     */
    ApplicationRuleDTO toDTO(ApplicationRule applicationRule);

    /**
     * Converts an {@link ApplicationRuleView} entity to an {@link ApplicationRuleViewDTO}.
     *
     * @param applicationRuleView the application rule view entity
     * @return the corresponding DTO
     */
    ApplicationRuleViewDTO toDTO(ApplicationRuleView applicationRuleView);
}
