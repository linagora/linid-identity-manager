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

package io.github.linagora.linid.im.api.persistence.interceptor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.linagora.linid.im.api.config.SuperAdminConfig;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: RlsInterceptor")
class RlsInterceptorTest {

    @Mock
    private EntityManager entityManager;

    @Mock(answer = Answers.RETURNS_SELF)
    private Query query;

    private RlsInterceptor rlsInterceptor;

    @BeforeEach
    void setUp() {
        rlsInterceptor = new RlsInterceptor(entityManager, new SuperAdminConfig("admin-sub"));
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    @DisplayName("test securize: should bind the id of the user, who is not the SUPER_ADMIN")
    void testSecurize() {
        var userPrincipal = userPrincipal(false);
        givenActiveTransaction();

        rlsInterceptor.securize(userPrincipal);

        verify(query).setParameter("accountId", userPrincipal.getId().toString());
        verify(query).setParameter("isSuperAdmin", "false");
        verify(query).setParameter("superAdminSub", "admin-sub");
        verify(query).getSingleResult();
    }

    @Test
    @DisplayName("test securize: should bind the id of the user and the SUPER_ADMIN flag")
    void testSecurizeSuperAdmin() {
        var userPrincipal = userPrincipal(true);
        givenActiveTransaction();

        rlsInterceptor.securize(userPrincipal);

        verify(query).setParameter("accountId", userPrincipal.getId().toString());
        verify(query).setParameter("isSuperAdmin", "true");
        verify(query).setParameter("superAdminSub", "admin-sub");
        verify(query).getSingleResult();
    }

    @Test
    @DisplayName("test securizeAsSystem: should bind an empty account id with full access")
    void testSecurizeAsSystem() {
        givenActiveTransaction();

        rlsInterceptor.securizeAsSystem();

        verify(query).setParameter("accountId", "");
        verify(query).setParameter("isSuperAdmin", "true");
        verify(query).setParameter("superAdminSub", "admin-sub");
        verify(query).getSingleResult();
    }

    @Test
    @DisplayName("test securize: should set the variables for the current transaction only")
    void testSecurizeSetsLocalVariables() {
        givenActiveTransaction();

        rlsInterceptor.securize(userPrincipal(false));

        var sql = ArgumentCaptor.forClass(String.class);
        verify(entityManager).createNativeQuery(sql.capture());
        // The third argument of set_config, is_local, keeps each variable within the current transaction.
        assertTrue(sql.getValue().contains("set_config('linid.account_id', :accountId, true)"));
        assertTrue(sql.getValue().contains("set_config('linid.is_super_admin', :isSuperAdmin, true)"));
        assertTrue(sql.getValue().contains("set_config('linid.super_admin_sub', :superAdminSub, true)"));
    }

    @Test
    @DisplayName("test securize: should throw an exception outside of a transaction")
    void testSecurizeWithoutTransaction() {
        var userPrincipal = userPrincipal(false);

        assertThrows(IllegalStateException.class, () -> rlsInterceptor.securize(userPrincipal));
        verifyNoInteractions(entityManager);
    }

    @Test
    @DisplayName("test securizeAsSystem: should throw an exception outside of a transaction")
    void testSecurizeAsSystemWithoutTransaction() {
        assertThrows(IllegalStateException.class, () -> rlsInterceptor.securizeAsSystem());
        verifyNoInteractions(entityManager);
    }

    private void givenActiveTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
    }

    private static UserPrincipal userPrincipal(final boolean superAdmin) {
        var userPrincipal = new UserPrincipal();
        userPrincipal.setId(UUID.randomUUID());
        userPrincipal.setSuperAdmin(superAdmin);
        return userPrincipal;
    }
}
