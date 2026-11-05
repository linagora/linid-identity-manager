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

package io.github.linagora.linid.im.sync.batch.rights;

import io.github.linagora.linid.im.sync.persistence.model.AccountComputedRightView;
import io.github.linagora.linid.im.sync.persistence.model.ComputedRight;
import io.github.linagora.linid.im.sync.service.AccountLockService;
import io.github.linagora.linid.im.sync.service.AlertService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.ChunkListener;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.stereotype.Component;

/**
 * Listener of the steps computing the rights: takes the locks of the accounts of each chunk before its pairs are
 * computed, logs the {@code pairError} alert of each pair skipped, and logs at the end of the step the number of pairs
 * written, in error and discarded.
 *
 * <p>The numbers are the counters of the step: the pairs written are its write count, the pairs in error, skipped,
 * its process skip count, and the pairs discarded because they changed since they were read, filtered out by
 * {@link ComputeRightsProcessor}, its filter count.</p>
 */
@Slf4j
@Component
public class ComputeRightsStepListener implements StepExecutionListener,
    SkipListener<AccountComputedRightView, ComputedRight>, ChunkListener<AccountComputedRightView, ComputedRight> {

    /**
     * Service taking the locks of the accounts of a chunk.
     */
    private final AccountLockService accountLockService;

    /**
     * Creates the listener of the steps computing the rights.
     *
     * @param accountLockService the service taking the locks of the accounts of a chunk
     */
    public ComputeRightsStepListener(final AccountLockService accountLockService) {
        this.accountLockService = accountLockService;
    }

    /**
     * Takes the locks of the accounts of the chunk, once its pairs are read, in its transaction: they are held while
     * {@link ComputeRightsProcessor} computes the pairs, checks their checksums and reads their current rows, and
     * while the writer writes them, until the commit of the chunk.
     *
     * <p>The locks are taken for the whole chunk, not pair by pair by the processor: the accounts would then be locked
     * in the order of the pairs, not of the keys, and two processings could take the same keys in different orders and
     * deadlock.</p>
     *
     * <p>Spring Batch also calls this method with the rows written, in the scan mode that follows a skippable write
     * error: the steps never enter it, since they only skip the pairs in error during the processing.</p>
     *
     * @param chunk the pairs of the chunk
     */
    @Override
    public void beforeChunk(final Chunk<AccountComputedRightView> chunk) {
        accountLockService.lockAccounts(chunk.getItems().stream().map(AccountComputedRightView::getAccountId).toList());
    }

    /**
     * Logs the {@code pairError} alert for a pair skipped, with the message of its root cause and without its stack
     * trace: an error of the claims template of an application puts all its pairs in error, and a stack trace per pair
     * would flood the logs at each cycle.
     *
     * @param item the pair in error
     * @param t    the cause of the error
     */
    @Override
    public void onSkipInProcess(final AccountComputedRightView item, final Throwable t) {
        AlertService.pairError("account=" + item.getExternalId() + ", application=" + item.getApplicationCode()
            + ", cause=" + ExceptionUtils.getRootCauseMessage(t), null);
    }

    /**
     * Logs the number of pairs written, in error and discarded during the step.
     *
     * @param stepExecution the execution of the step
     * @return {@code null}, to leave the exit status of the step unchanged
     */
    @Override
    public ExitStatus afterStep(final StepExecution stepExecution) {
        log.info("Rights computation of step {}: {} written, {} in error, {} discarded", stepExecution.getStepName(),
            stepExecution.getWriteCount(), stepExecution.getProcessSkipCount(), stepExecution.getFilterCount());
        return null;
    }
}
