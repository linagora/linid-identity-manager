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
import java.util.List;
import java.util.UUID;
import org.springframework.batch.infrastructure.item.database.AbstractPagingItemReader;
import org.springframework.data.domain.Limit;

/**
 * Reader of {@code allAccountsPayloadStep}: reads every account of {@code account_computed_rights_view} once, except
 * the technical account of {@code sync-api}, by pages, and gives the snapshots of each page, loaded by
 * {@link AccountSnapshotLoader}: the accounts with the checksums of their current payloads and their rows in each
 * source.
 *
 * <p>Each page starts after the last account of the previous one, not at an offset: an account deleted since the
 * previous page is no longer in the view, without shifting the next accounts. With pages of the chunk size, a page is
 * read at the first read of a chunk, in its transaction, in which the loader locks its accounts.</p>
 *
 * <p>The reader keeps no state for a restart: each cycle is a new job instance, whose step reads every account from
 * the first one, so that every payload is built from the current sources. Its state is not saved either: by default,
 * a restarted {@code AbstractPagingItemReader} would skip the number of accounts read before, which shifts with the
 * accounts added or deleted in between.</p>
 */
public class AllAccountSnapshotsReader extends AbstractPagingItemReader<AccountSnapshot> {

    /**
     * Lowest UUID, bound of the first page: PostgreSQL orders it before every other UUID.
     */
    private static final UUID LOWEST_ACCOUNT_ID = new UUID(0L, 0L);

    /**
     * Repository of {@code account_computed_rights_view}, giving the pages of accounts.
     */
    private final AccountComputedRightViewRepository viewRepository;

    /**
     * Identifier of the technical account of {@code sync-api}, never read.
     */
    private final UUID technicalAccountId;

    /**
     * Loader of the snapshots of the accounts of a page.
     */
    private final AccountSnapshotLoader loader;

    /**
     * Last account of the pages read, after which the next page starts.
     */
    private UUID lastPageAccountId = LOWEST_ACCOUNT_ID;

    /**
     * Creates the reader of the snapshots of every account.
     *
     * @param viewRepository     the repository of {@code account_computed_rights_view}
     * @param technicalAccountId the identifier of the technical account of {@code sync-api}, never read
     * @param pageSize           the number of accounts per page
     * @param loader             the loader of the snapshots of the accounts of a page
     */
    public AllAccountSnapshotsReader(final AccountComputedRightViewRepository viewRepository,
                                     final UUID technicalAccountId,
                                     final int pageSize,
                                     final AccountSnapshotLoader loader) {
        this.viewRepository = viewRepository;
        this.technicalAccountId = technicalAccountId;
        this.loader = loader;
        setPageSize(pageSize);
        setSaveState(false);
    }

    /**
     * Reads the next page: the next accounts of the view, then their snapshots, loaded once they are locked.
     *
     * @throws IllegalStateException naming the source when a source cannot be read, after logging the
     *     {@code sourceUnreadable} alert
     */
    @Override
    protected void doReadPage() {
        List<AccountIdentity> page = viewRepository
            .findDistinctByAccountIdNotAndAccountIdGreaterThanOrderByAccountIdAsc(
                technicalAccountId, lastPageAccountId, Limit.of(getPageSize()));
        if (!page.isEmpty()) {
            lastPageAccountId = page.getLast().accountId();
        }
        results = loader.load(page);
    }
}
