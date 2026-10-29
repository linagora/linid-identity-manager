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

package io.github.linagora.linid.im.sync.config;

import org.apache.commons.lang3.StringUtils;

/**
 * Configuration of a source of the account payloads.
 *
 * <p>The default is set in the constructor rather than with {@code @DefaultValue}: a source written without settings,
 * such as {@code accounts: {}}, is bound with an empty column name, not with a missing one.</p>
 *
 * @param accountIdColumn column of the source holding the {@code act_id} of the account,
 *     {@value #DEFAULT_ACCOUNT_ID_COLUMN} when it is not set
 */
public record SourceProperties(String accountIdColumn) {

    /**
     * Account id column of a source when it is not set.
     */
    public static final String DEFAULT_ACCOUNT_ID_COLUMN = "act_id";

    /**
     * Creates the settings of a source, with {@value #DEFAULT_ACCOUNT_ID_COLUMN} as account id column when it is not
     * set.
     *
     * @param accountIdColumn column of the source holding the {@code act_id} of the account, or a blank value
     */
    public SourceProperties {
        accountIdColumn = StringUtils.defaultIfBlank(accountIdColumn, DEFAULT_ACCOUNT_ID_COLUMN);
    }
}
