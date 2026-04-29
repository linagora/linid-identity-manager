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

import io.github.linagora.linid.im.api.model.account.AccountActivationRecord;
import io.github.linagora.linid.im.api.model.account.AccountRecord;
import io.github.linagora.linid.im.api.model.account.AccountStatusMapper;
import io.github.linagora.linid.im.api.model.account.AccountStatusRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.Account;
import io.github.linagora.linid.im.api.persistence.model.AccountStatus;
import io.github.linagora.linid.im.api.persistence.model.AccountView;
import io.github.linagora.linid.im.api.persistence.model.AccountViewQueryFilterDto;
import io.github.linagora.linid.im.api.persistence.repository.AccountRepository;
import io.github.linagora.linid.im.api.persistence.repository.AccountStatusRepository;
import io.github.linagora.linid.im.api.persistence.repository.AccountViewRepository;
import io.github.linagora.linid.im.api.service.validation.AccountActivationValidator;
import io.github.linagora.linid.im.api.service.validation.AccountStatusValidator;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: AccountServiceImpl")
class AccountServiceImplTest {

    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountViewRepository accountViewRepository;
    @Mock
    private ChecksumService checksumService;
    @Mock
    private AccountStatusRepository accountStatusRepository;
    @Mock
    private AccountStatusMapper accountStatusMapper;
    @Mock
    private AccountActivationValidator accountActivationValidator;
    @Mock
    private AccountStatusValidator accountStatusValidator;
    @InjectMocks
    private AccountServiceImpl accountService;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        userPrincipal = new UserPrincipal();
        userPrincipal.setId(ADMIN_ID);
        userPrincipal.setEmail("admin@example.com");
    }

    @Test
    @DisplayName("Should create account with correct fields and checksum")
    void testCreate_shouldSetAllFieldsAndChecksum() {
        var request = new AccountRecord("ext-001", "Doe", "John", "john@example.com");
        when(checksumService.compute("{}")).thenReturn(
            "44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a");
        when(accountRepository.save(any(Account.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.create(userPrincipal, request);

        assertNotNull(result);
        assertEquals("ext-001", result.getExternalId());
        assertEquals("Doe", result.getLastname());
        assertEquals("John", result.getFirstname());
        assertEquals("john@example.com", result.getEmail());
        assertEquals("{}", result.getPayload());
        assertEquals(ADMIN_ID, result.getCreatedBy());
        assertEquals(ADMIN_ID, result.getUpdatedBy());
        assertEquals("44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a",
            result.getChecksum());
    }

    @Test
    @DisplayName("Should generate consistent SHA-256 checksum for default payload")
    void testCreate_shouldGenerateConsistentChecksum() {
        var request = new AccountRecord("ext-001", "Doe", "John", "john@example.com");
        when(checksumService.compute("{}")).thenReturn("fixed-checksum");
        when(accountRepository.save(any(Account.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Account first = accountService.create(userPrincipal, request);
        Account second = accountService.create(userPrincipal, request);

        assertEquals(first.getChecksum(), second.getChecksum());
    }

    @Test
    @DisplayName("Should call repository with specification and pageable")
    void testFindAll_shouldDelegateToRepository() {
        var pageable = PageRequest.of(0, 10);
        var entity = new AccountView();
        var filters = new AccountViewQueryFilterDto();
        when(accountViewRepository.findAll(
            ArgumentMatchers.<Specification<AccountView>>any(),
            any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(entity)));

        Page<AccountView> result = accountService.findAll(userPrincipal, filters, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(accountViewRepository).findAll(
            ArgumentMatchers.<Specification<AccountView>>any(),
            any(Pageable.class));
    }

    @Test
    @DisplayName("Should return account when found by ID")
    void testFindById_shouldReturnAccountWhenFound() {
        UUID id = UUID.randomUUID();
        var entity = new AccountView();
        entity.setId(id);
        when(accountViewRepository.findById(id)).thenReturn(Optional.of(entity));

        AccountView result = accountService.findById(userPrincipal, id);

        assertNotNull(result);
        assertEquals(id, result.getId());
    }

    @Test
    @DisplayName("Should throw ApiException 404 when account not found")
    void testFindById_shouldThrow404WhenNotFound() {
        UUID id = UUID.randomUUID();
        when(accountViewRepository.findById(id)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class,
            () -> accountService.findById(userPrincipal, id));

        assertEquals(404, exception.getStatusCode());
        assertEquals("error.account.not_found", exception.getError().key());
    }

    @Test
    @DisplayName("Should delete account when it exists")
    void testDeleteById_shouldDeleteWhenFound() {
        UUID id = UUID.randomUUID();
        var entity = new AccountView();
        entity.setId(id);
        when(accountViewRepository.findById(id)).thenReturn(Optional.of(entity));

        accountService.deleteById(userPrincipal, id);

        verify(accountRepository).deleteById(id);
    }

    @Test
    @DisplayName("Should throw ApiException 404 when deleting non-existent account")
    void testDeleteById_shouldThrow404WhenNotFound() {
        UUID id = UUID.randomUUID();
        when(accountViewRepository.findById(id)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class,
            () -> accountService.deleteById(userPrincipal, id));

        assertEquals(404, exception.getStatusCode());
        assertEquals("error.account.not_found", exception.getError().key());
        verify(accountRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Should get account by email")
    void testGetAccountByEmail_shouldReturnOptional() {
        UUID id = UUID.randomUUID();
        var entity = new Account();
        entity.setId(id);
        when(accountRepository.findAccountByEmail("email")).thenReturn(Optional.of(entity));

        var account = accountService.getAccountByEmail("email");

        assertTrue(account.isPresent());
        assertEquals(entity, account.get());
    }

    @Test
    @DisplayName("Should get empty account on null email")
    void testGetAccountByEmail_shouldReturnOptionalOnNull() {
        assertTrue(accountService.getAccountByEmail(null).isEmpty());
        assertTrue(accountService.getAccountByEmail("").isEmpty());
        assertTrue(accountService.getAccountByEmail("  ").isEmpty());
    }

    @Test
    @DisplayName("updateStatus should create a new status row when none exists and persist it")
    void testUpdateStatus_shouldCreateRowWhenNoneExists() {
        UUID id = UUID.randomUUID();
        var view = new AccountView();
        view.setId(id);
        var record = new AccountStatusRecord(null, null, null, "REASON", null, null);

        when(accountRepository.existsById(id)).thenReturn(true);
        when(accountViewRepository.findById(id)).thenReturn(Optional.of(view));
        when(accountStatusRepository.findByAccountId(id)).thenReturn(Optional.empty());
        when(accountStatusRepository.saveAndFlush(any(AccountStatus.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AccountView result = accountService.updateStatus(userPrincipal, id, record);

        assertNotNull(result);
        verify(accountStatusMapper).update(any(AccountStatus.class), eq(record));
        verify(accountStatusRepository).saveAndFlush(any(AccountStatus.class));
    }

    @Test
    @DisplayName("updateStatus should update existing status row")
    void testUpdateStatus_shouldUpdateExistingRow() {
        UUID id = UUID.randomUUID();
        var view = new AccountView();
        view.setId(id);
        var existing = new AccountStatus();
        existing.setAccountId(id);
        var record = new AccountStatusRecord(null, null, null, null, null, null);

        when(accountRepository.existsById(id)).thenReturn(true);
        when(accountViewRepository.findById(id)).thenReturn(Optional.of(view));
        when(accountStatusRepository.findByAccountId(id)).thenReturn(Optional.of(existing));
        when(accountStatusRepository.saveAndFlush(existing)).thenReturn(existing);

        accountService.updateStatus(userPrincipal, id, record);

        verify(accountStatusMapper).update(existing, record);
        assertEquals(ADMIN_ID, existing.getUpdatedBy());
        verify(accountStatusRepository).saveAndFlush(existing);
    }

    @Test
    @DisplayName("updateStatus should throw 404 when account not found")
    void testUpdateStatus_shouldThrow404WhenAccountNotFound() {
        UUID id = UUID.randomUUID();
        when(accountRepository.existsById(id)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class,
            () -> accountService.updateStatus(userPrincipal, id,
                new AccountStatusRecord(null, null, null, null, null, null)));

        assertEquals(404, ex.getStatusCode());
        assertEquals("error.account.not_found", ex.getError().key());
        verify(accountStatusRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("updateStatus should propagate the exception thrown by the status validator")
    void testUpdateStatus_shouldPropagateValidatorFailure() {
        UUID id = UUID.randomUUID();
        var existing = new AccountStatus();
        var record = new AccountStatusRecord(null, null, null, null, null, null);

        when(accountRepository.existsById(id)).thenReturn(true);
        when(accountStatusRepository.findByAccountId(id)).thenReturn(Optional.of(existing));
        doThrow(new ApiException(HttpStatus.BAD_REQUEST.value(),
            I18nMessage.of("error.account.status.validity_end_in_past",
                Map.of("id", id.toString(), "end", "x"))))
            .when(accountStatusValidator).validate(eq(existing), eq(record), eq(id));

        ApiException ex = assertThrows(ApiException.class,
            () -> accountService.updateStatus(userPrincipal, id, record));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.status.validity_end_in_past", ex.getError().key());
        verify(accountStatusMapper, never()).update(any(), any());
        verify(accountStatusRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("updateStatus should invoke the validator before mapping and saving")
    void testUpdateStatus_shouldInvokeValidatorBeforeMapping() {
        UUID id = UUID.randomUUID();
        var view = new AccountView();
        view.setId(id);
        var existing = new AccountStatus();
        existing.setAccountId(id);
        var record = new AccountStatusRecord(null, null, null, null, null, null);

        when(accountRepository.existsById(id)).thenReturn(true);
        when(accountViewRepository.findById(id)).thenReturn(Optional.of(view));
        when(accountStatusRepository.findByAccountId(id)).thenReturn(Optional.of(existing));
        when(accountStatusRepository.saveAndFlush(existing)).thenReturn(existing);

        accountService.updateStatus(userPrincipal, id, record);

        var inOrder = inOrder(accountStatusValidator, accountStatusMapper, accountStatusRepository);
        inOrder.verify(accountStatusValidator).validate(existing, record, id);
        inOrder.verify(accountStatusMapper).update(existing, record);
        inOrder.verify(accountStatusRepository).saveAndFlush(existing);
    }

    @Test
    @DisplayName("updateActivation should throw 404 when no status row exists, without invoking the validator")
    void testUpdateActivation_shouldThrow404WhenNoStatus() {
        UUID id = UUID.randomUUID();

        when(accountRepository.existsById(id)).thenReturn(true);
        when(accountStatusRepository.findByAccountId(id)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
            () -> accountService.updateActivation(userPrincipal, id,
                new AccountActivationRecord(OffsetDateTime.now())));

        assertEquals(404, ex.getStatusCode());
        assertEquals("error.account.status.not_found", ex.getError().key());
        verify(accountActivationValidator, never()).validate(any(), any(), any());
    }

    @Test
    @DisplayName("updateActivation should propagate the exception thrown by the validator")
    void testUpdateActivation_shouldPropagateValidatorFailure() {
        UUID id = UUID.randomUUID();
        var existing = new AccountStatus();

        when(accountRepository.existsById(id)).thenReturn(true);
        when(accountStatusRepository.findByAccountId(id)).thenReturn(Optional.of(existing));
        doThrow(new ApiException(HttpStatus.BAD_REQUEST.value(),
            I18nMessage.of("error.account.status.activation.already_activated",
                Map.of("id", id.toString()))))
            .when(accountActivationValidator).validate(eq(existing), any(), eq(id));

        ApiException ex = assertThrows(ApiException.class,
            () -> accountService.updateActivation(userPrincipal, id,
                new AccountActivationRecord(OffsetDateTime.now())));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.status.activation.already_activated", ex.getError().key());
        verify(accountStatusRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("updateActivation should set activationAt and save on happy path")
    void testUpdateActivation_shouldSucceedOnHappyPath() {
        UUID id = UUID.randomUUID();
        var view = new AccountView();
        view.setId(id);
        var existing = new AccountStatus();
        OffsetDateTime activationAt = OffsetDateTime.now().minusHours(1);

        when(accountRepository.existsById(id)).thenReturn(true);
        when(accountViewRepository.findById(id)).thenReturn(Optional.of(view));
        when(accountStatusRepository.findByAccountId(id)).thenReturn(Optional.of(existing));
        when(accountStatusRepository.saveAndFlush(existing)).thenReturn(existing);

        AccountView result = accountService.updateActivation(userPrincipal, id,
            new AccountActivationRecord(activationAt));

        assertNotNull(result);
        assertEquals(activationAt, existing.getActivationAt());
        assertEquals(ADMIN_ID, existing.getUpdatedBy());
        verify(accountActivationValidator).validate(eq(existing), any(), eq(id));
        verify(accountStatusRepository).saveAndFlush(existing);
    }

    @Test
    @DisplayName("updateActivation should throw 404 when account not found")
    void testUpdateActivation_shouldThrow404WhenAccountNotFound() {
        UUID id = UUID.randomUUID();
        when(accountRepository.existsById(id)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class,
            () -> accountService.updateActivation(userPrincipal, id,
                new AccountActivationRecord(OffsetDateTime.now())));

        assertEquals(404, ex.getStatusCode());
        assertEquals("error.account.not_found", ex.getError().key());
        verify(accountActivationValidator, never()).validate(any(), any(), any());
    }
}
