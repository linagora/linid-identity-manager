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

import io.github.linagora.linid.im.sync.model.payload.AccountSnapshot;
import io.github.linagora.linid.im.sync.model.payload.PayloadOutcome;
import io.github.linagora.linid.im.sync.persistence.model.AccountPayload;
import io.github.linagora.linid.im.sync.persistence.repository.AccountComputedRightViewRepository;
import io.github.linagora.linid.im.sync.persistence.repository.AccountPayloadRepository;
import io.github.linagora.linid.im.sync.service.PayloadService;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Processor of {@code buildPayloadsStep}: builds the payload of an account and its checksum with
 * {@link PayloadService}, and gives the row to write when the checksum changed.
 *
 * <p>An unchanged payload is filtered out, compared with the checksum read with the page of the account, without
 * reading its row. An account deleted since its page was read is also filtered out: its payload is deleted with it, and
 * inserting it again would fail on the foreign key. Both are counted in the filter count of the step. An account in
 * error raises a {@link io.github.linagora.linid.im.sync.exception.PayloadException}, skipped by the step, which logs
 * the {@code payloadError} alert: the account keeps its previous payload.</p>
 *
 * <p>The outcome of each account is counted with {@link PayloadStepListener#count(PayloadOutcome)}.</p>
 */
@Component
public class PayloadProcessor implements ItemProcessor<AccountSnapshot, AccountPayload> {

    /**
     * Service building the payloads.
     */
    private final PayloadService payloadService;

    /**
     * Repository of {@code account_computed_rights_view}, telling which accounts still exist.
     */
    private final AccountComputedRightViewRepository viewRepository;

    /**
     * Repository of {@code account_payloads}, holding the current payloads.
     */
    private final AccountPayloadRepository payloadRepository;

    /**
     * Identifier of the technical account of {@code sync-api}, the author of the rows written.
     */
    private final UUID technicalAccountId;

    /**
     * Identifier of the {@code sync-api} instance, written with the payloads.
     */
    private final String instanceId;

    /**
     * Creates the processor of the payloads.
     *
     * @param payloadService     the service building the payloads
     * @param viewRepository     the repository of {@code account_computed_rights_view}
     * @param payloadRepository  the repository of {@code account_payloads}
     * @param technicalAccountId the identifier of the technical account of {@code sync-api}
     * @param instanceId         the identifier of the {@code sync-api} instance
     */
    public PayloadProcessor(
        final PayloadService payloadService,
        final AccountComputedRightViewRepository viewRepository,
        final AccountPayloadRepository payloadRepository,
        @Value("${sync.technical-account-id}") final UUID technicalAccountId,
        @Value("${sync.instance-id}") final String instanceId) {
        this.payloadService = payloadService;
        this.viewRepository = viewRepository;
        this.payloadRepository = payloadRepository;
        this.technicalAccountId = technicalAccountId;
        this.instanceId = instanceId;
    }

    /**
     * Builds the payload of an account, and gives its row when its checksum changed: its current row updated, or a new
     * row when the account has no payload yet.
     *
     * @param item the account, with its rows and the checksum of its current payload
     * @return the row to write, {@code null} when the payload is unchanged or the account was deleted
     * @throws io.github.linagora.linid.im.sync.exception.PayloadException when the payload of the account cannot
     *     be built
     */
    @Override
    public AccountPayload process(final AccountSnapshot item) {
        Map<String, Object> payload = payloadService.build(item.rowsBySource());

        String checksum = payloadService.checksum(payload);
        if (checksum.equals(item.account().payloadChecksum())) {
            PayloadStepListener.count(PayloadOutcome.UNCHANGED);
            return null;
        }

        UUID accountId = item.account().actId();
        Optional<AccountPayload> current = currentRow(item);
        // Account deleted since its page was read: inserting its payload would fail on the foreign key
        if (isDeleted(item, current)) {
            PayloadStepListener.count(PayloadOutcome.DELETED);
            return null;
        }

        if (current.isPresent()) {
            PayloadStepListener.count(PayloadOutcome.UPDATED);
        } else {
            PayloadStepListener.count(PayloadOutcome.INSERTED);
        }
        return rowToWrite(current, accountId, payload, checksum);
    }

    /**
     * Reads the current row of an account, only when its page gave the checksum of a payload.
     *
     * @param item the account, with the checksum of its current payload
     * @return the current row, empty when the account has no payload, or was deleted since its page was read
     */
    private Optional<AccountPayload> currentRow(final AccountSnapshot item) {
        if (item.account().payloadChecksum() == null) {
            return Optional.empty();
        }
        return payloadRepository.findByActId(item.account().actId());
    }

    /**
     * Tells whether an account was deleted since its page was read: an account without payload is no longer in the
     * view, and an account with a payload no longer has its row.
     *
     * @param item    the account, with the checksum of its current payload
     * @param current the current row of the account, read by {@link #currentRow(AccountSnapshot)}
     * @return {@code true} when the account was deleted
     */
    private boolean isDeleted(final AccountSnapshot item, final Optional<AccountPayload> current) {
        if (item.account().payloadChecksum() == null) {
            return !viewRepository.existsByActId(item.account().actId());
        }
        // The payload is deleted with its account
        return current.isEmpty();
    }

    /**
     * Gives the row to write for a changed payload: the current row of the account, or a new row when it has none,
     * with the payload, its checksum, the instance, the job execution and the technical account as updater.
     *
     * @param current   the current row of the account, empty when it has no payload yet
     * @param accountId the identifier of the account
     * @param payload   the payload of the account
     * @param checksum  the checksum of the payload
     * @return the row to write
     */
    private AccountPayload rowToWrite(final Optional<AccountPayload> current, final UUID accountId,
                                      final Map<String, Object> payload, final String checksum) {
        AccountPayload row = current.orElseGet(() -> newRow(accountId));
        row.setPayload(payload);
        row.setChecksum(checksum);
        row.setSyncInstanceId(instanceId);
        row.setSyncJobExecutionId(StepSynchronizationManager.getContext().getStepExecution().getJobExecutionId());
        row.setUpdatedBy(technicalAccountId);
        return row;
    }

    /**
     * Creates the row of an account without payload yet, with the technical account as author.
     *
     * @param accountId the identifier of the account
     * @return the new row
     */
    private AccountPayload newRow(final UUID accountId) {
        AccountPayload row = new AccountPayload();
        row.setActId(accountId);
        row.setCreatedBy(technicalAccountId);
        return row;
    }

}
