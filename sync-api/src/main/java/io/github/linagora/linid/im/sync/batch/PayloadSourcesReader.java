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
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
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
 * <p>The last account read is saved in the context of the step at each commit, so that a restarted step resumes
 * after it, whatever the accounts added or deleted in between. The comparison of the accounts is made by
 * PostgreSQL.</p>
 */
public class PayloadSourcesReader extends AbstractPagingItemReader<AccountSnapshot> {

    /**
     * Lowest UUID, bound of the first page: PostgreSQL orders it before every other UUID.
     */
    private static final UUID LOWEST_ACCOUNT_ID = new UUID(0L, 0L);

    /**
     * Key of the last account read, in the context of the step.
     */
    private static final String LAST_ACCOUNT_KEY = "last.account";

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
     * Last account returned, saved in the context of the step.
     */
    private UUID lastReadAccountId;

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
        setName("payloadSourcesReader");
    }

    /**
     * Restores the last account read by a previous execution of the step, after which the reading resumes.
     *
     * @param executionContext the context of the step
     */
    @Override
    public void open(final ExecutionContext executionContext) throws ItemStreamException {
        super.open(executionContext);
        String key = getExecutionContextKey(LAST_ACCOUNT_KEY);
        if (isSaveState() && executionContext.containsKey(key)) {
            lastPageAccountId = UUID.fromString(executionContext.getString(key));
            lastReadAccountId = lastPageAccountId;
        }
    }

    /**
     * Saves the last account read in the context of the step, at each commit.
     *
     * @param executionContext the context of the step
     */
    @Override
    public void update(final ExecutionContext executionContext) throws ItemStreamException {
        super.update(executionContext);
        if (isSaveState() && lastReadAccountId != null) {
            executionContext.putString(getExecutionContextKey(LAST_ACCOUNT_KEY), lastReadAccountId.toString());
        }
    }

    /**
     * Reads the next account, and keeps it as the last account read.
     *
     * @return the next account with its rows, {@code null} when every account was read
     * @throws Exception when a page cannot be read
     */
    @Override
    protected AccountSnapshot doRead() throws Exception {
        AccountSnapshot item = super.doRead();
        if (item != null) {
            lastReadAccountId = item.account().actId();
        }
        return item;
    }

    /**
     * Does not move by the number of items read: the reading resumes after the last account read, restored by
     * {@link #open(ExecutionContext)}.
     *
     * @param itemIndex the number of items read by the previous execution
     */
    @Override
    protected void jumpToItem(final int itemIndex) {
        // The position is the last account read, not a number of items
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
        List<AccountSummary> summaries = viewRepository.findDistinctByActIdNotAndActIdGreaterThanOrderByActIdAsc(
            technicalAccountId, lastPageAccountId, Limit.of(getPageSize()));
        if (summaries.isEmpty()) {
            results = List.of();
            return;
        }
        lastPageAccountId = summaries.getLast().actId();

        List<UUID> actIds = summaries.stream().map(AccountSummary::actId).toList();
        Map<String, Map<UUID, List<Map<String, Object>>>> pageRows = new HashMap<>();
        properties.sources().forEach((source, settings) ->
            pageRows.put(source, readSource(source, settings.accountIdColumn(), actIds)));

        results = summaries.stream()
            .map(summary -> new AccountSnapshot(summary, accountRows(summary.actId(), pageRows)))
            .toList();
    }

    /**
     * Gives the rows of an account in each source of its page, an empty list for a source without row.
     *
     * @param actId    the identifier of the account
     * @param pageRows the rows of the page, by source then by {@code act_id}
     * @return the rows of the account, by source name
     */
    private static Map<String, List<Map<String, Object>>> accountRows(
        final UUID actId,
        final Map<String, Map<UUID, List<Map<String, Object>>>> pageRows) {
        return pageRows.entrySet().stream()
            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey,
                entry -> entry.getValue().getOrDefault(actId, List.of())));
    }

    /**
     * Reads the rows of a source for the given accounts, and groups them by account.
     *
     * @param source          the name of the source
     * @param accountIdColumn the column of the source holding the {@code act_id} of the account
     * @param actIds          the identifiers of the accounts
     * @return the rows of the source by {@code act_id}, an account without row being absent
     * @throws IllegalStateException naming the source, after logging the {@code sourceUnreadable} alert
     */
    private Map<UUID, List<Map<String, Object>>> readSource(
        final String source,
        final String accountIdColumn,
        final Collection<UUID> actIds) {
        try {
            return sourceRepository.findAllByAccountIds(source, accountIdColumn, actIds).stream()
                .collect(Collectors.groupingBy(row -> (UUID) row.get(accountIdColumn)));
        } catch (RuntimeException e) {
            AlertService.sourceUnreadable("source=" + source, e);
            throw new IllegalStateException("Unable to read the source " + source, e);
        }
    }
}
