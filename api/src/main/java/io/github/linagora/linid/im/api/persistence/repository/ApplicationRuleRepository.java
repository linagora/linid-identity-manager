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

package io.github.linagora.linid.im.api.persistence.repository;

import io.github.linagora.linid.im.api.persistence.model.ApplicationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link ApplicationRule}.
 *
 * <p>Extends {@link JpaSpecificationExecutor} to support dynamic filtering
 * via {@code spring-query-filter} specifications.</p>
 */
public interface ApplicationRuleRepository extends JpaRepository<ApplicationRule, UUID>,
    JpaSpecificationExecutor<ApplicationRule> {

    /**
     * Retrieves an {@link ApplicationRule} by its identifier scoped to the given application.
     *
     * @param id            the rule identifier
     * @param applicationId the identifier of the owning application
     * @return an {@link Optional} containing the matching {@link ApplicationRule} if found,
     * or {@link Optional#empty()} otherwise
     */
    Optional<ApplicationRule> findByIdAndApplicationId(UUID id, UUID applicationId);

    /**
     * Checks whether a rule with the given code already exists within the application.
     *
     * @param applicationId the identifier of the owning application
     * @param code          the functional code of the rule
     * @return {@code true} if a matching rule exists, {@code false} otherwise
     */
    boolean existsByApplicationIdAndCode(UUID applicationId, String code);

    /**
     * Checks whether another rule (different from the given identifier) with the given code already exists
     * within the application.
     *
     * @param applicationId the identifier of the owning application
     * @param code          the functional code of the rule
     * @param id            the identifier of the rule to exclude from the check
     * @return {@code true} if a matching rule exists, {@code false} otherwise
     */
    boolean existsByApplicationIdAndCodeAndIdNot(UUID applicationId, String code, UUID id);
}
