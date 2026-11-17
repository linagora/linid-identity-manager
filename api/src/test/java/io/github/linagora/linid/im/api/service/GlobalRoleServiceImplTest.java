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

package io.github.linagora.linid.im.api.service;

import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.GlobalRoleEnum;
import io.github.linagora.linid.im.api.persistence.repository.AccountRepository;
import io.github.linagora.linid.im.api.persistence.repository.GlobalRoleRepository;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: GlobalRoleServiceImpl")
class GlobalRoleServiceImplTest {

    @Mock
    private GlobalRoleRepository globalRoleRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private GlobalRoleServiceImpl service;

    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        userPrincipal = new UserPrincipal();
        userPrincipal.setId(UUID.randomUUID());
        userPrincipal.setEmail("admin@example.com");
    }

    @Test
    @DisplayName("create should assign the role to the account")
    void testCreate_shouldAssignRole() {
        var accountId = UUID.randomUUID();
        when(accountRepository.existsById(accountId)).thenReturn(true);
        when(globalRoleRepository.existsByAccountIdAndRole(accountId, GlobalRoleEnum.ACCOUNTS_ADMIN))
            .thenReturn(false);
        when(globalRoleRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(userPrincipal, accountId, "ACCOUNTS_ADMIN");

        assertEquals(accountId, result.getAccountId());
        assertEquals(GlobalRoleEnum.ACCOUNTS_ADMIN, result.getRole());
        assertEquals(userPrincipal.getId(), result.getCreatedBy());
        assertEquals(userPrincipal.getId(), result.getUpdatedBy());
    }

    @Test
    @DisplayName("create should throw a 400 when the role is SUPER_ADMIN, which is declared by configuration")
    void testCreate_shouldThrowWhenRoleIsSuperAdmin() {
        var exception = assertThrows(ApiException.class,
            () -> service.create(userPrincipal, UUID.randomUUID(), "SUPER_ADMIN"));

        assertEquals(400, exception.getStatusCode());
        assertEquals("error.global_role.unknown", exception.getError().key());
        verify(globalRoleRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("create should throw a 400 when the role is unknown")
    void testCreate_shouldThrowWhenRoleUnknown() {
        var exception = assertThrows(ApiException.class,
            () -> service.create(userPrincipal, UUID.randomUUID(), "accounts_admin"));

        assertEquals(400, exception.getStatusCode());
        assertEquals("error.global_role.unknown", exception.getError().key());
        verify(globalRoleRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("create should throw a 404 when the account does not exist")
    void testCreate_shouldThrowWhenAccountAbsent() {
        var accountId = UUID.randomUUID();
        when(accountRepository.existsById(accountId)).thenReturn(false);

        var exception = assertThrows(ApiException.class,
            () -> service.create(userPrincipal, accountId, "GROUPS_AUDITOR"));

        assertEquals(404, exception.getStatusCode());
        assertEquals("error.account.not_found", exception.getError().key());
        verify(globalRoleRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("create should throw a 400 when the account already holds the role")
    void testCreate_shouldThrowWhenRoleAlreadyAssigned() {
        var accountId = UUID.randomUUID();
        when(accountRepository.existsById(accountId)).thenReturn(true);
        when(globalRoleRepository.existsByAccountIdAndRole(accountId, GlobalRoleEnum.ROLES_ADMIN)).thenReturn(true);

        var exception = assertThrows(ApiException.class,
            () -> service.create(userPrincipal, accountId, "ROLES_ADMIN"));

        assertEquals(400, exception.getStatusCode());
        assertEquals("error.global_role.already_assigned", exception.getError().key());
        verify(globalRoleRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("create should throw a 400 when a concurrent request assigned the role first")
    void testCreate_shouldThrowWhenRoleAssignedConcurrently() {
        var accountId = UUID.randomUUID();
        when(accountRepository.existsById(accountId)).thenReturn(true);
        when(globalRoleRepository.existsByAccountIdAndRole(accountId, GlobalRoleEnum.ROLES_ADMIN)).thenReturn(false);
        when(globalRoleRepository.saveAndFlush(any()))
            .thenThrow(new DataIntegrityViolationException("uk_global_roles_act_id_role"));

        var exception = assertThrows(ApiException.class,
            () -> service.create(userPrincipal, accountId, "ROLES_ADMIN"));

        assertEquals(400, exception.getStatusCode());
        assertEquals("error.global_role.already_assigned", exception.getError().key());
    }
}
