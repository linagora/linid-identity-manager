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

package io.github.linagora.linid.im.api.service.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.linagora.linid.im.corelib.exception.ApiException;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Test class: PeriodValidator")
class PeriodValidatorTest {

    private static final String ERROR_KEY = "error.test.period_invalid";

    private PeriodValidator validator;
    private OffsetDateTime start;
    private OffsetDateTime end;

    @BeforeEach
    void setUp() {
        this.validator = new PeriodValidator();
        this.start = OffsetDateTime.parse("2025-01-01T00:00:00Z");
        this.end = OffsetDateTime.parse("2026-01-01T00:00:00Z");
    }

    @Test
    @DisplayName("ensureCoherent should not throw when start is before end")
    void testEnsureCoherent_shouldAcceptStartBeforeEnd() {
        assertDoesNotThrow(() -> validator.ensureCoherent(start, end, ERROR_KEY));
    }

    @Test
    @DisplayName("ensureCoherent should accept boundary equality (start == end)")
    void testEnsureCoherent_shouldAcceptBoundary() {
        assertDoesNotThrow(() -> validator.ensureCoherent(start, start, ERROR_KEY));
    }

    @Test
    @DisplayName("ensureCoherent should skip when start is null")
    void testEnsureCoherent_shouldSkipWhenStartNull() {
        assertDoesNotThrow(() -> validator.ensureCoherent(null, end, ERROR_KEY));
    }

    @Test
    @DisplayName("ensureCoherent should skip when end is null")
    void testEnsureCoherent_shouldSkipWhenEndNull() {
        assertDoesNotThrow(() -> validator.ensureCoherent(start, null, ERROR_KEY));
    }

    @Test
    @DisplayName("ensureCoherent should throw 400 when start is after end")
    void testEnsureCoherent_shouldThrowWhenStartAfterEnd() {
        ApiException ex = assertThrows(ApiException.class,
            () -> validator.ensureCoherent(end, start, ERROR_KEY));

        assertEquals(400, ex.getStatusCode());
        assertEquals(ERROR_KEY, ex.getError().key());
    }

    @Test
    @DisplayName("ensureCoherent overload should forward initial context entries into the i18n message")
    void testEnsureCoherent_shouldForwardContext() {
        ApiException ex = assertThrows(ApiException.class,
            () -> validator.ensureCoherent(end, start, ERROR_KEY, Map.of("id", "abc")));

        assertEquals(400, ex.getStatusCode());
        assertEquals(ERROR_KEY, ex.getError().key());
    }
}
