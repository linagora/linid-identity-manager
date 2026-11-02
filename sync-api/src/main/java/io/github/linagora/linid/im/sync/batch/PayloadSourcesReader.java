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

package io.github.linagora.linid.im.sync.batch;

import io.github.linagora.linid.im.sync.config.SyncDataProperties;
import io.github.linagora.linid.im.sync.model.payload.AccountSnapshot;
import io.github.linagora.linid.im.sync.persistence.model.AccountSummary;
import io.github.linagora.linid.im.sync.persistence.repository.AccountComputedRightViewRepository;
import io.github.linagora.linid.im.sync.persistence.repository.SourceRepository;
import io.github.linagora.linid.im.sync.service.AlertService;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.batch.infrastructure.item.database.AbstractPagingItemReader;
import org.springframework.data.domain.Limit;

/**
 * Reader of {@code buildPayloadsStep}: reads the accounts of {@code account_computed_rights_view} by pages, with the
 * checksums of their current payloads, and adds to each one its rows in each source.
 *
 * <p>Each page starts after the last account of the previous one, not at an offset: an account deleted since the
 * previous page is no longer in the view, without shifting the next accounts. For each page, each source is read once
 * for all its accounts. With pages of the chunk size, a page is read at the first read of a chunk, in its transaction.
 * An account with several rows in a source is given as it is: the processor rejects it, so that the alert names the
 * account.</p>
 *
 * <p>The reader keeps no state for a restart: each cycle is a new job instance, whose step reads every account from
 * the first one, so that every payload is built from the current sources. Its state is not saved either: by default,
 * a restarted {@code AbstractPagingItemReader} would skip the number of accounts read before, which shifts with the
 * accounts added or deleted in between.</p>
 */
public class PayloadSourcesReader extends AbstractPagingItemReader<AccountSnapshot> {

    /**
     * Lowest UUID, bound of the first page: PostgreSQL orders it before every other UUID.
     */
    private static final UUID LOWEST_ACCOUNT_ID = new UUID(0L, 0L);

    /**
     * Repository of {@code account_computed_rights_view}, giving the accounts and the checksums of their payloads.
     */
    private final AccountComputedRightViewRepository viewRepository;

    /**
     * Identifier of the technical account of {@code sync-api}, whose payload is never built.
     */
    private final UUID technicalAccountId;

    /**
     * Configuration holding the sources.
     */
    private final SyncDataProperties properties;

    /**
     * Repository reading the rows of a source.
     */
    private final SourceRepository sourceRepository;

    /**
     * Last account of the pages read, after which the next page starts.
     */
    private UUID lastPageAccountId = LOWEST_ACCOUNT_ID;

    /**
     * Creates the reader of the accounts and their sources.
     *
     * @param viewRepository     the repository of {@code account_computed_rights_view}
     * @param technicalAccountId the identifier of the technical account of {@code sync-api}
     * @param pageSize           the number of accounts per page
     * @param properties         the configuration holding the sources
     * @param sourceRepository   the repository reading the rows of a source
     */
    public PayloadSourcesReader(
        final AccountComputedRightViewRepository viewRepository,
        final UUID technicalAccountId,
        final int pageSize,
        final SyncDataProperties properties,
        final SourceRepository sourceRepository) {
        this.viewRepository = viewRepository;
        this.technicalAccountId = technicalAccountId;
        this.properties = properties;
        this.sourceRepository = sourceRepository;
        setPageSize(pageSize);
        setSaveState(false);
    }

    /**
     * Reads the next page: the next accounts of the view with the checksums of their current payloads, and their rows
     * in each source.
     *
     * @throws IllegalStateException naming the source when a source cannot be read, after logging the
     *     {@code sourceUnreadable} alert
     */
    @Override
    protected void doReadPage() {
        List<AccountSummary> summaries = viewRepository
            .findDistinctByAccountIdNotAndAccountIdGreaterThanOrderByAccountIdAsc(
                technicalAccountId, lastPageAccountId, Limit.of(getPageSize()));
        if (summaries.isEmpty()) {
            results = List.of();
            return;
        }
        lastPageAccountId = summaries.getLast().accountId();

        List<UUID> accountIds = summaries.stream().map(AccountSummary::accountId).toList();
        Map<String, Map<UUID, List<Map<String, Object>>>> pageRows = new HashMap<>();
        properties.sources().forEach((source, settings) ->
            pageRows.put(source, readSource(source, settings.accountIdColumn(), accountIds)));

        results = summaries.stream()
            .map(summary -> new AccountSnapshot(summary, accountRows(summary.accountId(), pageRows)))
            .toList();
    }

    /**
     * Gives the rows of an account in each source of its page, an empty list for a source without row.
     *
     * @param accountId the identifier of the account
     * @param pageRows  the rows of the page, by source then by {@code act_id}
     * @return the rows of the account, by source name
     */
    private static Map<String, List<Map<String, Object>>> accountRows(
        final UUID accountId,
        final Map<String, Map<UUID, List<Map<String, Object>>>> pageRows) {
        return pageRows.entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,
                entry -> entry.getValue().getOrDefault(accountId, List.of())));
    }

    /**
     * Reads the rows of a source for the given accounts, and groups them by account.
     *
     * @param source          the name of the source
     * @param accountIdColumn the column of the source holding the {@code act_id} of the account
     * @param accountIds      the identifiers of the accounts
     * @return the rows of the source by {@code act_id}, an account without row being absent
     * @throws IllegalStateException naming the source, after logging the {@code sourceUnreadable} alert
     */
    private Map<UUID, List<Map<String, Object>>> readSource(
        final String source,
        final String accountIdColumn,
        final Collection<UUID> accountIds) {
        try {
            return sourceRepository.findAllByAccountIds(source, accountIdColumn, accountIds).stream()
                .collect(Collectors.groupingBy(row -> (UUID) row.get(accountIdColumn)));
        } catch (RuntimeException e) {
            AlertService.sourceUnreadable("source=" + source, e);
            throw new IllegalStateException("Unable to read the source " + source, e);
        }
    }
}
