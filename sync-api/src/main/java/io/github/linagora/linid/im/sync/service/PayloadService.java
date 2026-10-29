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

import java.util.List;
import java.util.Map;

/**
 * Builds the account payloads from the {@code mapping} of the {@code sync-data.job} section.
 */
public interface PayloadService {

    /**
     * Checks the {@code mapping} by rendering it with an empty context, where each source gives {@code null} through
     * {@code tojson}, so that a valid {@code mapping} gives a JSON object.
     *
     * @throws IllegalStateException when the {@code mapping} is invalid, such as a Jinjava syntax error or a value
     *     inserted without {@code tojson}, after logging the {@code invalidMapping} alert
     */
    void checkMapping();

    /**
     * Builds the payload of an account: the {@code mapping} is rendered with, for each source, a variable named after
     * it holding the columns of the row of the account, empty when it has no row, and parsed as a JSON object.
     *
     * @param rowsBySource the rows of the account in each source, by source name
     * @return the payload of the account
     * @throws io.github.linagora.linid.im.sync.exception.PayloadException when the account has several rows in a
     *     source, the template raises an error, or the rendered text is not a JSON object
     */
    Map<String, Object> build(Map<String, List<Map<String, Object>>> rowsBySource);
}
