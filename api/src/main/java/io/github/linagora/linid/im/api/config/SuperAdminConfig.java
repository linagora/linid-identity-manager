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

package io.github.linagora.linid.im.api.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Declaration of the SUPER_ADMIN through configuration.
 *
 * <p>The account whose {@code external_id}, the {@code sub} claim of its tokens, equals {@code LINID_SUPER_ADMIN_SUB}
 * is the SUPER_ADMIN. Nothing is written to the database: removing the variable then restarting the API revokes the
 * role, for example once an administrators manager is named.</p>
 */
@Slf4j
@Configuration
public class SuperAdminConfig {

    /**
     * Subject of the SUPER_ADMIN, blank when no SUPER_ADMIN is declared.
     */
    private final String sub;

    /**
     * Creates the configuration of the SUPER_ADMIN.
     *
     * @param sub the subject of the SUPER_ADMIN, blank when no SUPER_ADMIN is declared
     */
    public SuperAdminConfig(@Value("${super-admin.sub}") final String sub) {
        this.sub = sub;
    }

    /**
     * Logs a warning at each startup while a SUPER_ADMIN is declared, so that a declaration left behind shows up.
     */
    @PostConstruct
    public void warnWhenDeclared() {
        if (StringUtils.isNotBlank(sub)) {
            log.warn("A SUPER_ADMIN is declared by LINID_SUPER_ADMIN_SUB: the account whose external_id is '{}' has "
                + "every right. Remove the variable once an administrators manager is named.", sub);
        }
    }
}
