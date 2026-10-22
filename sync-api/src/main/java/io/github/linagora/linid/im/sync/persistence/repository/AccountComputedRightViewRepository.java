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

import io.github.linagora.linid.im.sync.persistence.model.AccountApplicationPairId;
import io.github.linagora.linid.im.sync.persistence.model.AccountComputedRightView;
import io.github.linagora.linid.im.sync.persistence.model.AccountIdentity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.Repository;

/**
 * Read-only Spring Data repository for {@link AccountComputedRightView}.
 *
 * <p>Exposes only read operations: mutating methods from {@code JpaRepository} (save, delete, flush...) are
 * intentionally not inherited, since {@link AccountComputedRightView} is backed by a database view.</p>
 */
public interface AccountComputedRightViewRepository
    extends Repository<AccountComputedRightView, AccountApplicationPairId> {

    /**
     * Finds every account of the view once, except the technical account of {@code sync-api}, ordered by its
     * identifier.
     *
     * <p>The {@link AccountIdentity} projection only selects the identifiers of the accounts, so that the distinct
     * applies to them and not to the whole rows. The result is a {@link List}, read before the chunks of the payload
     * build: a JPA stream cannot stay open across their transactions.</p>
     *
     * <p>The technical account gets no payload, so its pairs are never {@code to_compute} and are never selected for
     * the computation of the rights.</p>
     *
     * @param technicalAccountId the identifier of the technical account of {@code sync-api}
     * @return the identifiers of every other account, including the accounts never computed, empty when no
     *     application is deployed
     */
    List<AccountIdentity> findDistinctByActIdNotOrderByActIdAsc(UUID technicalAccountId);
}
