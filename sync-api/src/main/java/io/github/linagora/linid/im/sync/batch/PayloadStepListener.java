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
import io.github.linagora.linid.im.sync.service.AlertService;
import io.github.linagora.linid.im.sync.service.PayloadService;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.ChunkListener;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.stereotype.Component;

/**
 * Listener of {@code buildPayloadsStep}: checks the {@code mapping} before the step, logs the {@code payloadError}
 * alert of each account skipped, and logs the number of accounts of each {@link PayloadOutcome} at the end of the
 * step.
 *
 * <p>The accounts are counted by {@link #count(PayloadOutcome)}, in the execution context of the step. The accounts
 * of a chunk that fails are not counted: its rows are rolled back, and the counters, saved with the step even when it
 * fails, are restored to their values at the start of the chunk.</p>
 */
@Slf4j
@Component
public class PayloadStepListener implements StepExecutionListener, SkipListener<AccountSnapshot, AccountPayload>,
    ChunkListener<AccountSnapshot, AccountPayload> {

    /**
     * Attribute of the step context holding the counters of the outcomes at the start of the current chunk.
     */
    private static final String CHUNK_START_COUNTS = "payloads.chunk-start-counts";

    /**
     * Service building the payloads.
     */
    private final PayloadService payloadService;

    /**
     * Creates the listener of the payload build.
     *
     * @param payloadService the service building the payloads
     */
    public PayloadStepListener(final PayloadService payloadService) {
        this.payloadService = payloadService;
    }

    /**
     * Checks the {@code mapping} before the step reads any account, so that an invalid {@code mapping} stops it before
     * any source is read.
     *
     * @param stepExecution the execution of the step
     * @throws IllegalStateException when the {@code mapping} is invalid, after logging the {@code invalidMapping}
     *     alert, which makes the step fail
     */
    @Override
    public void beforeStep(final StepExecution stepExecution) {
        payloadService.checkMapping();
    }

    /**
     * Keeps the counters of the outcomes at the start of the chunk, once its accounts are read, so that they can be
     * restored if the chunk fails.
     *
     * @param chunk the accounts of the chunk
     */
    @Override
    public void beforeChunk(final Chunk<AccountSnapshot> chunk) {
        StepContext stepContext = StepSynchronizationManager.getContext();
        ExecutionContext context = stepContext.getStepExecution().getExecutionContext();
        Map<PayloadOutcome, Integer> counts = new EnumMap<>(PayloadOutcome.class);
        for (PayloadOutcome outcome : PayloadOutcome.values()) {
            counts.put(outcome, context.getInt(outcome.key(), 0));
        }
        stepContext.setAttribute(CHUNK_START_COUNTS, counts);
    }

    /**
     * Forgets the counters kept at the start of the chunk, once its rows are written.
     *
     * @param chunk the rows written
     */
    @Override
    public void afterChunk(final Chunk<AccountPayload> chunk) {
        StepSynchronizationManager.getContext().removeAttribute(CHUNK_START_COUNTS);
    }

    /**
     * Restores the counters of the outcomes kept at the start of the chunk, whose rows are rolled back. Nothing is
     * restored when the chunk fails while its accounts are read: none of them is counted yet.
     *
     * @param exception the cause of the failure
     * @param chunk     the rows of the chunk
     */
    @Override
    public void onChunkError(final Exception exception, final Chunk<AccountPayload> chunk) {
        StepContext stepContext = StepSynchronizationManager.getContext();
        @SuppressWarnings("unchecked")
        Map<PayloadOutcome, Integer> counts =
            (Map<PayloadOutcome, Integer>) stepContext.removeAttribute(CHUNK_START_COUNTS);
        if (counts != null) {
            ExecutionContext context = stepContext.getStepExecution().getExecutionContext();
            counts.forEach((outcome, count) -> context.putInt(outcome.key(), count));
        }
    }

    /**
     * Logs the {@code payloadError} alert for an account skipped, and counts it.
     *
     * @param item the account in error
     * @param t    the cause of the error
     */
    @Override
    public void onSkipInProcess(final AccountSnapshot item, final Throwable t) {
        AlertService.payloadError("account=" + item.account().externalId(), t);
        count(PayloadOutcome.IN_ERROR);
    }

    /**
     * Logs the number of accounts of each {@link PayloadOutcome} during the step.
     *
     * @param stepExecution the execution of the step
     * @return {@code null}, to leave the exit status of the step unchanged
     */
    @Override
    public ExitStatus afterStep(final StepExecution stepExecution) {
        ExecutionContext context = stepExecution.getExecutionContext();
        String summary = Arrays.stream(PayloadOutcome.values())
            .map(outcome -> context.getInt(outcome.key(), 0) + " " + outcome.label())
            .collect(Collectors.joining(", "));
        log.info("Payload build of step {}: {}", stepExecution.getStepName(), summary);
        return null;
    }

    /**
     * Counts an account of the given outcome, in the execution context of the current step.
     *
     * @param outcome the outcome of the account
     */
    public static void count(final PayloadOutcome outcome) {
        ExecutionContext context = StepSynchronizationManager.getContext().getStepExecution().getExecutionContext();
        context.putInt(outcome.key(), context.getInt(outcome.key(), 0) + 1);
    }
}
