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

import io.github.linagora.linid.im.sync.config.SyncDataProperties;
import io.github.linagora.linid.im.sync.model.payload.AccountSnapshot;
import io.github.linagora.linid.im.sync.persistence.model.AccountChecksum;
import io.github.linagora.linid.im.sync.persistence.model.AccountIdentity;
import io.github.linagora.linid.im.sync.persistence.repository.AccountPayloadRepository;
import io.github.linagora.linid.im.sync.persistence.repository.SourceRepository;
import io.github.linagora.linid.im.sync.service.AccountLockService;
import io.github.linagora.linid.im.sync.service.AlertService;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Loader of the snapshots of the payload build: locks the given accounts, then reads the checksums of their current
 * payloads, in {@code account_payloads}, and their rows in each source, each source being read once for all the
 * accounts.
 *
 * <p>Shared by the readers of the payload build, {@link AllAccountSnapshotsReader} for every account and
 * {@link AccountSnapshotReader} for one account, which call it at the first read of a chunk, in its transaction: the
 * locks are held until the commit of the chunk, so that two processings of the same account read and write one after
 * the other, the later one reading the fresher data.</p>
 *
 * <p>An account with several rows in a source is given as it is: the processor rejects it, so that the alert names the
 * account.</p>
 */
@Component
public class AccountSnapshotLoader {

    /**
     * Repository of {@code account_payloads}, giving the checksums of the payloads of the accounts.
     */
    private final AccountPayloadRepository payloadRepository;

    /**
     * Configuration holding the sources.
     */
    private final SyncDataProperties properties;

    /**
     * Repository reading the rows of a source.
     */
    private final SourceRepository sourceRepository;

    /**
     * Service taking the locks of the accounts.
     */
    private final AccountLockService accountLockService;

    /**
     * Creates the loader of the snapshots.
     *
     * @param payloadRepository  the repository of {@code account_payloads}
     * @param properties         the configuration holding the sources
     * @param sourceRepository   the repository reading the rows of a source
     * @param accountLockService the service taking the locks of the accounts
     */
    public AccountSnapshotLoader(final AccountPayloadRepository payloadRepository,
                                 final SyncDataProperties properties,
                                 final SourceRepository sourceRepository,
                                 final AccountLockService accountLockService) {
        this.payloadRepository = payloadRepository;
        this.properties = properties;
        this.sourceRepository = sourceRepository;
        this.accountLockService = accountLockService;
    }

    /**
     * Loads the snapshots of the given accounts: locks them, then reads the checksums of their current payloads and
     * their rows in each source.
     *
     * <p>The checksums are read under the locks: a processing that held them may have written the payloads of the
     * accounts since they were read in the view, and the processor would compare the new payloads with outdated
     * checksums. An account without checksum has no payload yet, or was deleted since it was read: the processor tells
     * them apart. Every account is given, in the given order, so that a page of accounts keeps its size, since a page
     * shorter than the page size ends the reading.</p>
     *
     * @param accounts the accounts read in {@code account_computed_rights_view}
     * @return the snapshots of the accounts, in the same order, empty without any query when there is no account
     * @throws IllegalStateException naming the source when a source cannot be read, after logging the
     *     {@code sourceUnreadable} alert
     */
    public List<AccountSnapshot> load(final List<AccountIdentity> accounts) {
        if (accounts.isEmpty()) {
            return List.of();
        }
        List<UUID> accountIds = accounts.stream().map(AccountIdentity::accountId).toList();
        accountLockService.lockAccounts(accountIds);
        Map<UUID, String> checksums = payloadRepository.findByAccountIdIn(accountIds).stream()
            .collect(Collectors.toMap(AccountChecksum::accountId, AccountChecksum::checksum));

        Map<String, Map<UUID, List<Map<String, Object>>>> sourceRows = new HashMap<>();
        properties.sources().forEach((source, settings) ->
            sourceRows.put(source, readSource(source, settings.accountIdColumn(), accountIds)));

        return accounts.stream()
            .map(account -> new AccountSnapshot(account, checksums.get(account.accountId()),
                accountRows(account.accountId(), sourceRows)))
            .toList();
    }

    /**
     * Loads the snapshot of one account, like {@link #load(List)}: locks it, then reads the checksum of its current
     * payload and its rows in each source.
     *
     * @param account the account read in {@code account_computed_rights_view}
     * @return the snapshot of the account
     * @throws IllegalStateException naming the source when a source cannot be read, after logging the
     *     {@code sourceUnreadable} alert
     */
    public AccountSnapshot load(final AccountIdentity account) {
        return load(List.of(account)).getFirst();
    }

    /**
     * Gives the rows of an account in each source, an empty list for a source without row.
     *
     * @param accountId  the identifier of the account
     * @param sourceRows the rows of the accounts loaded, by source then by {@code act_id}
     * @return the rows of the account, by source name
     */
    private static Map<String, List<Map<String, Object>>> accountRows(
        final UUID accountId,
        final Map<String, Map<UUID, List<Map<String, Object>>>> sourceRows) {
        return sourceRows.entrySet().stream()
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
