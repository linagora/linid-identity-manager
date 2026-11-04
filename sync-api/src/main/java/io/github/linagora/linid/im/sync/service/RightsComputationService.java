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

package io.github.linagora.linid.im.sync.service;

import io.github.linagora.linid.im.sync.model.rights.ComputedRightRecord;
import io.github.linagora.linid.im.sync.persistence.model.AccountComputedRightView;
import java.util.Set;

/**
 * Service computing the rights of an account and application pair: its OPA result and its claims.
 */
public interface RightsComputationService {

    /**
     * Computes the rights of a pair: evaluates the policy of the application with the payload of the account, checks
     * the roles returned against the roles of the application, and renders the claims template of the application.
     *
     * <p>A role missing from the roles of the application raises the {@code unknownRole} alert, and the rights are
     * given anyway.</p>
     *
     * @param pair      the pair, with the payload of the account, the code and the claims template of the application
     * @param roleNames the names of the roles of the application
     * @return the rights of the pair
     * @throws io.github.linagora.linid.im.sync.exception.RightsComputationException when the rights of the pair cannot
     *     be computed: failed evaluation, template error, rendered text that is not a JSON object
     * @throws io.github.linagora.linid.im.sync.exception.OpaUnreachableException when the OPA server cannot be
     *     reached, after {@link OpaService} logged the {@code opaUnreachable} alert
     */
    ComputedRightRecord compute(AccountComputedRightView pair, Set<String> roleNames);
}
