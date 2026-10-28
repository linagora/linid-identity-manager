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

package io.github.linagora.linid.im.sync.persistence.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reads the rows of a source of the account payloads, a table or a view of the linid-api database.
 *
 * <p>Not a Spring Data repository: the sources are only known at runtime, from the configuration, and have no
 * entity.</p>
 */
public interface SourceRepository {

    /**
     * Finds the rows of a source whose account id column holds one of the given {@code act_id}.
     *
     * <p>The source and column names are checked against {@code [a-z_][a-z0-9_]*} and quoted: an invalid name is
     * rejected with an {@link IllegalArgumentException} before any query.</p>
     *
     * @param source          the name of the table or view
     * @param accountIdColumn the column of the source holding the {@code act_id} of the account
     * @param actIds          the identifiers of the accounts
     * @return the rows found, each mapping the name of each column to its value, a PostgreSQL array being read as a
     *     {@link List}, and a {@code json} or {@code jsonb} value as parsed JSON
     */
    List<Map<String, Object>> findAllByAccountIds(String source, String accountIdColumn, Collection<UUID> actIds);
}
