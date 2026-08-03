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
import static org.mockito.Mockito.when;

import io.github.linagora.linid.im.api.persistence.model.Application;
import io.github.linagora.linid.im.api.persistence.repository.ApplicationRepository;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: SystemApplicationValidator")
class SystemApplicationValidatorTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private SystemApplicationValidator validator;

    @Test
    @DisplayName("ensureApplicationIsMutable should throw for the system-reserved application")
    void testEnsureApplicationIsMutable_shouldThrowForSystemApplication() {
        var application = Application.builder().code("LINID").build();

        var exception = assertThrows(ApiException.class,
            () -> validator.ensureApplicationIsMutable(application));

        assertEquals(400, exception.getStatusCode());
        assertEquals("error.application.system_reserved", exception.getError().key());
        assertEquals("LINID", exception.getError().context().get("code"));
    }

    @Test
    @DisplayName("ensureApplicationIsMutable should accept any other application")
    void testEnsureApplicationIsMutable_shouldAcceptRegularApplication() {
        var application = Application.builder().code("my-app").build();

        assertDoesNotThrow(() -> validator.ensureApplicationIsMutable(application));
    }

    @Test
    @DisplayName("ensureApplicationIsMutable should be case-sensitive on the code")
    void testEnsureApplicationIsMutable_shouldBeCaseSensitive() {
        var application = Application.builder().code("linid").build();

        assertDoesNotThrow(() -> validator.ensureApplicationIsMutable(application));
    }

    @Test
    @DisplayName("ensureRolesAreMutable should throw for the roles of the system-reserved application")
    void testEnsureRolesAreMutable_shouldThrowForSystemApplication() {
        var applicationId = UUID.randomUUID();
        when(applicationRepository.findById(applicationId))
            .thenReturn(Optional.of(Application.builder().code("LINID").build()));

        var exception = assertThrows(ApiException.class,
            () -> validator.ensureRolesAreMutable(applicationId));

        assertEquals(400, exception.getStatusCode());
        assertEquals("error.application_role.system_reserved", exception.getError().key());
        assertEquals("LINID", exception.getError().context().get("code"));
    }

    @Test
    @DisplayName("ensureRolesAreMutable should accept the roles of any other application")
    void testEnsureRolesAreMutable_shouldAcceptRegularApplication() {
        var applicationId = UUID.randomUUID();
        when(applicationRepository.findById(applicationId))
            .thenReturn(Optional.of(Application.builder().code("my-app").build()));

        assertDoesNotThrow(() -> validator.ensureRolesAreMutable(applicationId));
    }

    @Test
    @DisplayName("ensureRolesAreMutable should accept an unknown application and let the caller report it")
    void testEnsureRolesAreMutable_shouldAcceptUnknownApplication() {
        var applicationId = UUID.randomUUID();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> validator.ensureRolesAreMutable(applicationId));
    }
}
