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

package io.github.linagora.linid.im.api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

@DisplayName("Test class: SuperAdminConfig")
class SuperAdminConfigTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(SuperAdminConfig.class);

    private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

    @BeforeEach
    void setUp() {
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logAppender);
    }

    @Test
    @DisplayName("test warnWhenDeclared: should log a warning naming the SUPER_ADMIN when one is declared")
    void testWarnWhenDeclaredWithSub() {
        new SuperAdminConfig("jdoe").warnWhenDeclared();

        assertEquals(1, logAppender.list.size());
        ILoggingEvent event = logAppender.list.getFirst();
        assertEquals(Level.WARN, event.getLevel());
        assertEquals("A SUPER_ADMIN is declared by LINID_SUPER_ADMIN_SUB: the account whose external_id is 'jdoe' has "
            + "every right. Remove the variable once an administrators manager is named.", event.getFormattedMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    @DisplayName("test warnWhenDeclared: should not log anything when no SUPER_ADMIN is declared")
    void testWarnWhenDeclaredWithoutSub(final String sub) {
        new SuperAdminConfig(sub).warnWhenDeclared();

        assertTrue(logAppender.list.isEmpty());
    }
}
