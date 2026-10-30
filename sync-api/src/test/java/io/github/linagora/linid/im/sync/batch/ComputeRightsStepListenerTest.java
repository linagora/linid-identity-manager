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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.linagora.linid.im.sync.exception.RightsComputationException;
import io.github.linagora.linid.im.sync.persistence.model.AccountComputedRightView;
import io.github.linagora.linid.im.sync.service.AlertService;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.test.MetaDataInstanceFactory;

@DisplayName("Test class: ComputeRightsStepListener")
class ComputeRightsStepListenerTest {

    private final Logger alertLogger = (Logger) LoggerFactory.getLogger(AlertService.class);

    private final Logger listenerLogger = (Logger) LoggerFactory.getLogger(ComputeRightsStepListener.class);

    private final ComputeRightsStepListener listener = new ComputeRightsStepListener();

    private ListAppender<ILoggingEvent> alertAppender;

    private ListAppender<ILoggingEvent> listenerAppender;

    private StepExecution stepExecution;

    @BeforeEach
    void setUp() {
        stepExecution = MetaDataInstanceFactory.createStepExecution(
            MetaDataInstanceFactory.createJobExecution(42L), "computeRightsStep", 7L);
        alertAppender = new ListAppender<>();
        alertAppender.start();
        alertLogger.addAppender(alertAppender);
        listenerAppender = new ListAppender<>();
        listenerAppender.start();
        listenerLogger.addAppender(listenerAppender);
    }

    @AfterEach
    void tearDown() {
        alertLogger.detachAppender(alertAppender);
        listenerLogger.detachAppender(listenerAppender);
    }

    @Test
    @DisplayName("onSkipInProcess should log the pairError alert of the pair, with its cause")
    void testOnSkipInProcess() {
        var pair = new AccountComputedRightView(UUID.fromString("00000000-0000-4000-8000-000000000001"), "jdoe",
            UUID.fromString("00000000-0000-4000-8000-0000000000a1"), "APP", "{}", Map.of(), "payload-checksum",
            "script-checksum", "claims-template-checksum", true);

        listener.onSkipInProcess(pair, new RightsComputationException("Unable to compute the rights of the pair",
            new IllegalStateException("Unclosed tag")));

        assertEquals(1, alertAppender.list.size());
        var alert = alertAppender.list.getFirst();
        assertEquals(Level.WARN, alert.getLevel());
        assertEquals("Pair in error, nothing written — account=jdoe, application=APP", alert.getFormattedMessage());
        assertEquals(RightsComputationException.class.getName(), alert.getThrowableProxy().getClassName());
    }

    @Test
    @DisplayName("afterStep should log the number of pairs written, skipped in error and filtered out as discarded, "
        + "leaving the exit status unchanged")
    void testAfterStep() {
        stepExecution.setWriteCount(5);
        stepExecution.setProcessSkipCount(2);
        stepExecution.setFilterCount(1);

        assertNull(listener.afterStep(stepExecution));

        assertEquals(1, listenerAppender.list.size());
        assertEquals(Level.INFO, listenerAppender.list.getFirst().getLevel());
        assertEquals("Rights computation of step computeRightsStep: 5 written, 2 in error, 1 discarded",
            listenerAppender.list.getFirst().getFormattedMessage());
    }
}
