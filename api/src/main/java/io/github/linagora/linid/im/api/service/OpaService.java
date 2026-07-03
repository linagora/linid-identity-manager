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

import io.github.linagora.linid.im.api.persistence.model.Application;
import io.github.linagora.linid.im.api.persistence.model.ApplicationRule;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Service managing the OPA policy workflow for applications.
 *
 * <p>It generates the policy script from the active rules of an application and publishes it to the OPA server.
 * Persisting the generated script on the application entity is the responsibility of the {@code ApplicationService},
 * not of this service.</p>
 */
public interface OpaService {

    /**
     * Generates the OPA policy script of an application from its active rules.
     *
     * @param application the owning application, used to build the Rego package namespace
     * @param activeRules the ordered list of active rules whose scripts are concatenated as fragments
     * @return the rendered Rego policy document
     */
    String generate(Application application, List<ApplicationRule> activeRules);

    /**
     * Publishes the policy script of the given application to the OPA server.
     *
     * @param application the application whose script must be published
     * @return the deployment timestamp recorded on success
     */
    OffsetDateTime publish(Application application);
}
