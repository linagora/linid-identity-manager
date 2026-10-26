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

import io.github.linagora.linid.im.sync.model.payload.PayloadItem;
import io.github.linagora.linid.im.sync.persistence.model.AccountIdentity;
import java.util.List;

/**
 * Builds the account payloads from the sources and the {@code mapping} of the {@code sync-data.job} section.
 */
public interface PayloadService {

    /**
     * Builds the payloads of the given accounts: each source is read once for all of them, then the {@code mapping} is
     * rendered for each account, with the row of the account in each source, and parsed as a JSON object.
     *
     * <p>The {@code mapping} is checked first, rendered with an empty context and parsed: when it is invalid, such as
     * a Jinjava syntax error or a value inserted without {@code tojson}, an {@link IllegalStateException} is raised,
     * after logging the {@code invalidMapping} alert, before any source is read. A source that cannot be read, such as
     * an unknown source, an invalid name, a missing account id column or a database error, raises an
     * {@link IllegalStateException} naming the source, after logging the {@code sourceUnreadable} alert.</p>
     *
     * <p>An account with several rows in a source, a template error or a rendered text that is not a JSON object is
     * left out, after logging the {@code payloadError} alert, and the payloads of the other accounts are built.</p>
     *
     * @param accounts the accounts whose payload is built
     * @return the payloads built, without the accounts in error
     */
    List<PayloadItem> build(List<AccountIdentity> accounts);
}
