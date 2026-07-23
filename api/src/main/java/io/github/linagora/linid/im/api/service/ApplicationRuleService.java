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

import io.github.linagora.linid.im.api.model.application.rule.ApplicationRuleRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.ApplicationRule;
import io.github.linagora.linid.im.api.persistence.model.ApplicationRuleView;
import io.github.linagora.linid.im.api.persistence.model.ApplicationRuleViewQueryFilterDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for application rule management operations.
 *
 * <p>All operations are scoped to an owning application, whose existence is enforced. The rule
 * {@code code} must be unique within the application. The {@code scriptChecksum} is computed from the
 * {@code script}; rules are always created disabled and their {@code disabled} state may only be changed
 * through an update.</p>
 */
public interface ApplicationRuleService {

    /**
     * Creates a new rule for the given application.
     *
     * <p>The rule is always created with {@code disabled = true}, regardless of the value carried by the
     * request. The {@code scriptChecksum} is computed from the {@code script}. The {@code code} must be
     * unique within the application.</p>
     *
     * @param userPrincipal the authenticated user
     * @param applicationId the identifier of the owning application
     * @param rule          the rule creation record
     * @return the created rule entity
     */
    ApplicationRule create(
        UserPrincipal userPrincipal,
        UUID applicationId,
        ApplicationRuleRecord rule);

    /**
     * Retrieves a paginated list of rules for the given application, optionally filtered.
     *
     * @param userPrincipal the authenticated user
     * @param applicationId the identifier of the owning application
     * @param filters       generated filter DTO from query parameters
     * @param pageable      pagination information
     * @return a page of application rule views
     */
    Page<ApplicationRuleView> findAll(
        UserPrincipal userPrincipal,
        UUID applicationId,
        ApplicationRuleViewQueryFilterDto filters,
        Pageable pageable);

    /**
     * Retrieves a rule entity by its identifier, ensuring it belongs to the given application.
     *
     * @param userPrincipal the authenticated user
     * @param applicationId the identifier of the owning application
     * @param id            the rule identifier
     * @return the rule entity
     */
    ApplicationRule findById(
        UserPrincipal userPrincipal,
        UUID applicationId,
        UUID id);

    /**
     * Retrieves an enriched rule view by its identifier, ensuring it belongs to the given application.
     *
     * @param userPrincipal the authenticated user
     * @param applicationId the identifier of the owning application
     * @param id            the rule identifier
     * @return the rule view entity
     */
    ApplicationRuleView findViewById(
        UserPrincipal userPrincipal,
        UUID applicationId,
        UUID id);

    /**
     * Updates the rule with the given identifier, ensuring it belongs to the given application.
     *
     * <p>All fields of the record are applied. The {@code scriptChecksum} is recomputed when the
     * {@code script} changes. The {@code code} must remain unique within the application.</p>
     *
     * @param userPrincipal the authenticated user
     * @param applicationId the identifier of the owning application
     * @param id            the rule identifier
     * @param rule          the update record
     * @return the updated rule entity
     */
    ApplicationRule update(
        UserPrincipal userPrincipal,
        UUID applicationId,
        UUID id,
        ApplicationRuleRecord rule);

    /**
     * Deletes a rule by its identifier, ensuring it belongs to the given application.
     *
     * @param userPrincipal the authenticated user
     * @param applicationId the identifier of the owning application
     * @param id            the rule identifier
     */
    void deleteById(
        UserPrincipal userPrincipal,
        UUID applicationId,
        UUID id);
}
