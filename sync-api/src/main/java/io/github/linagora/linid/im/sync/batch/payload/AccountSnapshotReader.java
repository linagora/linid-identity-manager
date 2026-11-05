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

package io.github.linagora.linid.im.sync.batch.payload;

import io.github.linagora.linid.im.sync.model.payload.AccountSnapshot;
import io.github.linagora.linid.im.sync.persistence.model.AccountIdentity;
import io.github.linagora.linid.im.sync.persistence.repository.AccountComputedRightViewRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.batch.infrastructure.item.ItemReader;

/**
 * Reader of {@code accountPayloadStep}: gives the snapshot of the account whose payload is recomputed, loaded by
 * {@link AccountSnapshotLoader}, then {@code null}.
 *
 * <p>The account is read in {@code account_computed_rights_view}, and its snapshot loaded, at the first read, not when
 * the reader is created: the first read happens in the transaction of the chunk, in which the loader locks the account
 * before reading the checksum of its payload and its rows in each source. An account without row in the view, since
 * no application is deployed, gives no snapshot. The technical account of {@code sync-api} gives none either, without
 * any query: its payload is never built, so that its pairs are never {@code to_compute}.</p>
 */
public class AccountSnapshotReader implements ItemReader<AccountSnapshot> {

    /**
     * Identifier of the account whose payload is recomputed.
     */
    private final UUID accountId;

    /**
     * Identifier of the technical account of {@code sync-api}, never read.
     */
    private final UUID technicalAccountId;

    /**
     * Repository of {@code account_computed_rights_view}, giving the account.
     */
    private final AccountComputedRightViewRepository viewRepository;

    /**
     * Loader of the snapshot of the account.
     */
    private final AccountSnapshotLoader loader;

    /**
     * Whether the account was already read, so that the next reads give {@code null}, which ends the step.
     */
    private boolean accountRead;

    /**
     * Creates the reader of the snapshot of one account.
     *
     * @param accountId          the identifier of the account whose payload is recomputed
     * @param technicalAccountId the identifier of the technical account of {@code sync-api}, never read
     * @param viewRepository     the repository of {@code account_computed_rights_view}
     * @param loader             the loader of the snapshot of the account
     */
    public AccountSnapshotReader(final UUID accountId,
                                 final UUID technicalAccountId,
                                 final AccountComputedRightViewRepository viewRepository,
                                 final AccountSnapshotLoader loader) {
        this.accountId = accountId;
        this.technicalAccountId = technicalAccountId;
        this.viewRepository = viewRepository;
        this.loader = loader;
    }

    /**
     * Gives the snapshot of the account at the first read, then {@code null}.
     *
     * @return the snapshot of the account, {@code null} once it was given, when the account has no row in the view or
     *     when it is the technical account
     * @throws IllegalStateException naming the source when a source cannot be read, after logging the
     *     {@code sourceUnreadable} alert
     */
    @Override
    public AccountSnapshot read() {
        if (accountRead || technicalAccountId.equals(accountId)) {
            return null;
        }
        accountRead = true;
        Optional<AccountIdentity> account = viewRepository.findDistinctByAccountId(accountId);
        if (account.isEmpty()) {
            return null;
        }
        return loader.load(account.get());
    }
}
