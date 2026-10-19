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

package io.github.linagora.linid.im.sync.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Named.named;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;

@DisplayName("Test class: AlertService")
class AlertServiceTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(AlertService.class);

    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logAppender);
    }

    static Stream<Arguments> alerts() {
        return Stream.of(
            Arguments.of(alert("invalidMapping", AlertService::invalidMapping), Level.ERROR,
                "Invalid mapping template, payload build aborted — account=a1"),
            Arguments.of(alert("sourceUnreadable", AlertService::sourceUnreadable), Level.ERROR,
                "Source unreadable, payload build aborted — account=a1"),
            Arguments.of(alert("opaUnreachable", AlertService::opaUnreachable), Level.ERROR,
                "OPA unreachable, authorization check aborted — account=a1"),
            Arguments.of(alert("payloadError", AlertService::payloadError), Level.WARN,
                "Account in error during payload build, keeping previous payload — account=a1"),
            Arguments.of(alert("unknownRole", AlertService::unknownRole), Level.WARN,
                "Unknown role returned by OPA, missing from application_roles — account=a1"),
            Arguments.of(alert("pairError", AlertService::pairError), Level.WARN,
                "Pair in error, nothing written — account=a1"));
    }

    private static Object alert(final String name, final BiConsumer<String, Throwable> alert) {
        return named(name, alert);
    }

    @ParameterizedTest(name = "{0} should log at {1} level")
    @MethodSource("alerts")
    @DisplayName("each alert should log its message, its context and its cause at the level of its blocking")
    void testAlert(final BiConsumer<String, Throwable> alert, final Level level, final String message) {
        alert.accept("account=a1", new IllegalStateException("cause"));

        assertEquals(1, logAppender.list.size());
        assertEquals(level, logAppender.list.getFirst().getLevel());
        assertEquals(message, logAppender.list.getFirst().getFormattedMessage());
        assertEquals("cause", logAppender.list.getFirst().getThrowableProxy().getMessage());
    }

    @Test
    @DisplayName("an alert should log a multi-line context on one line, and no cause when there is none")
    void testAlert_multiLineContextWithoutCause() {
        AlertService.unknownRole("account=a1\r\n role=r1\nrole=r2", null);

        assertEquals(1, logAppender.list.size());
        assertEquals("Unknown role returned by OPA, missing from application_roles — account=a1 role=r1 role=r2",
            logAppender.list.getFirst().getFormattedMessage());
        assertNull(logAppender.list.getFirst().getThrowableProxy());
    }
}
