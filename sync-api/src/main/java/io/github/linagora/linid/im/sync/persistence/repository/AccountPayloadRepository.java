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

import io.github.linagora.linid.im.sync.persistence.model.AccountChecksum;
import io.github.linagora.linid.im.sync.persistence.model.AccountPayload;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data repository for {@link AccountPayload}.
 */
public interface AccountPayloadRepository extends JpaRepository<AccountPayload, UUID> {

    /**
     * Finds the payload of an account.
     *
     * @param accountId the identifier of the account
     * @return the payload of the account, empty when it has none yet
     */
    Optional<AccountPayload> findByAccountId(UUID accountId);

    /**
     * Finds the checksums of the current payloads of the given accounts.
     *
     * <p>The payload build reads them once the accounts of a page are locked: a processing that held the locks may
     * have written their payloads since the page was read. The {@link AccountChecksum} projection only selects the
     * account and the checksum of its payload, through the unique index on {@code act_id}.</p>
     *
     * @param accountIds the identifiers of the accounts
     * @return the checksums of the accounts with a payload, in no particular order, an account without payload yet or
     *     deleted since being absent
     */
    List<AccountChecksum> findByAccountIdIn(Collection<UUID> accountIds);
}
