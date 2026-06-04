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
import io.github.linagora.linid.im.api.model.account.AccountDeactivationRecord;
import io.github.linagora.linid.im.api.model.account.AccountMapper;
import io.github.linagora.linid.im.api.model.account.AccountReactivationRecord;
import io.github.linagora.linid.im.api.model.account.AccountRecord;
import io.github.linagora.linid.im.api.model.account.AccountStatusMapperImpl;
import io.github.linagora.linid.im.api.model.account.AccountSuspensionRecord;
import io.github.linagora.linid.im.api.model.account.AccountValidityRecord;
import io.github.linagora.linid.im.api.model.common.CommonMapper;
import io.github.linagora.linid.im.api.model.common.PeriodRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.Account;
import io.github.linagora.linid.im.api.persistence.model.AccountStatus;
import io.github.linagora.linid.im.api.persistence.model.AccountView;
import io.github.linagora.linid.im.api.persistence.model.AccountViewQueryFilterDto;
import io.github.linagora.linid.im.api.persistence.model.OrganizationalUnitAccount;
import io.github.linagora.linid.im.api.persistence.repository.AccountRepository;
import io.github.linagora.linid.im.api.persistence.repository.AccountStatusRepository;
import io.github.linagora.linid.im.api.persistence.repository.AccountViewRepository;
import io.github.linagora.linid.im.api.persistence.repository.OrganizationalUnitAccountRepository;
import io.github.linagora.linid.im.api.persistence.repository.OrganizationalUnitRepository;
import io.github.linagora.linid.im.api.service.validation.AccountActivationValidator;
import io.github.linagora.linid.im.api.service.validation.AccountCreationValidator;
import io.github.linagora.linid.im.api.service.validation.AccountDeactivationValidator;
import io.github.linagora.linid.im.api.service.validation.AccountReactivationValidator;
import io.github.linagora.linid.im.api.service.validation.AccountSuspensionValidator;
import io.github.linagora.linid.im.api.service.validation.AccountValidityValidator;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.lang.reflect.Field;
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
    private static final OffsetDateTime START = OffsetDateTime.parse("2100-01-01T00:00:00Z");

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private AccountViewRepository accountViewRepository;
    @Mock
    private ChecksumService checksumService;
    @Mock
    private AccountStatusRepository accountStatusRepository;
    @Mock
    private OrganizationalUnitAccountRepository organizationalUnitAccountRepository;
    @Mock
    private OrganizationalUnitRepository organizationalUnitRepository;
    @Mock
    private AccountMapper accountMapper;
    @Spy
    private AccountStatusMapperImpl accountStatusMapper = new AccountStatusMapperImpl();
    @Spy
    private CommonMapper commonMapper = new CommonMapper();
    @Mock
    private AccountActivationValidator accountActivationValidator;
    @Mock
    private AccountSuspensionValidator accountSuspensionValidator;
    @Mock
    private AccountDeactivationValidator accountDeactivationValidator;
    @Mock
    private AccountReactivationValidator accountReactivationValidator;
    @Mock
    private AccountValidityValidator accountValidityValidator;
    @Mock
    private AccountCreationValidator accountCreationValidator;
    @InjectMocks
    private AccountServiceImpl accountService;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        Field generatedCommonMapper = AccountStatusMapperImpl.class.getDeclaredField("commonMapper");
        generatedCommonMapper.setAccessible(true);
        generatedCommonMapper.set(accountStatusMapper, commonMapper);
        userPrincipal = new UserPrincipal();
        userPrincipal.setId(ADMIN_ID);
        userPrincipal.setEmail("admin@example.com");
    }

    @Test
    @DisplayName("Should create account with correct fields and checksum")
    void testCreate_shouldSetAllFieldsAndChecksum() {
        UUID ouId = UUID.randomUUID();
        var request = new AccountRecord("ext-001", "Doe", "John", "john@example.com",
            new PeriodRecord(START, null), ouId);
        Account mappedAccount = new Account();
        mappedAccount.setExternalId("ext-001");
        mappedAccount.setLastname("Doe");
        mappedAccount.setFirstname("John");
        mappedAccount.setEmail("john@example.com");
        mappedAccount.setCreatedBy(ADMIN_ID);
        mappedAccount.setUpdatedBy(ADMIN_ID);
        when(accountMapper.toAccount(request, userPrincipal)).thenReturn(mappedAccount);
        when(checksumService.compute("{}")).thenReturn(
            "44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a");
        when(accountRepository.save(any(Account.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(accountStatusMapper.toAccountStatus(any(AccountRecord.class), any(UserPrincipal.class), any(Account.class)))
            .thenReturn(new AccountStatus());
        when(accountStatusRepository.save(any(AccountStatus.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(organizationalUnitAccountRepository.save(any(OrganizationalUnitAccount.class)))
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
        UUID ouId = UUID.randomUUID();
        var request = new AccountRecord("ext-001", "Doe", "John", "john@example.com",
            new PeriodRecord(START, null), ouId);
        when(accountMapper.toAccount(request, userPrincipal)).thenReturn(new Account());
        when(checksumService.compute("{}")).thenReturn("fixed-checksum");
        when(accountRepository.save(any(Account.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(accountStatusMapper.toAccountStatus(any(AccountRecord.class), any(UserPrincipal.class), any(Account.class)))
            .thenReturn(new AccountStatus());
        when(accountStatusRepository.save(any(AccountStatus.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(organizationalUnitAccountRepository.save(any(OrganizationalUnitAccount.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Account first = accountService.create(userPrincipal, request);
        Account second = accountService.create(userPrincipal, request);

        assertEquals(first.getChecksum(), second.getChecksum());
    }

    @Test
    @DisplayName("Should delegate validation to accountCreationValidator")
    void testCreate_shouldCallValidator() {
        UUID ouId = UUID.randomUUID();
        var request = new AccountRecord("ext-001", "Doe", "John", "john@example.com",
            new PeriodRecord(START, null), ouId);
        when(accountMapper.toAccount(request, userPrincipal)).thenReturn(new Account());
        when(checksumService.compute("{}")).thenReturn("fixed-checksum");
        when(accountRepository.save(any(Account.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(accountStatusMapper.toAccountStatus(any(AccountRecord.class), any(UserPrincipal.class), any(Account.class)))
            .thenReturn(new AccountStatus());
        when(accountStatusRepository.save(any(AccountStatus.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(organizationalUnitAccountRepository.save(any(OrganizationalUnitAccount.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        accountService.create(userPrincipal, request);

        verify(accountCreationValidator).validate(request);
    }

    @Test
    @DisplayName("Should not save account when validator throws")
    void testCreate_shouldNotSaveWhenValidatorThrows() {
        var request = new AccountRecord("ext-001", "Doe", "John", "john@example.com",
            new PeriodRecord(START, null), null);
        doThrow(new ApiException(HttpStatus.BAD_REQUEST.value(),
            I18nMessage.of("error.account.creation.validity_period_start_in_past")))
            .when(accountCreationValidator).validate(request);

        ApiException ex = assertThrows(ApiException.class,
            () -> accountService.create(userPrincipal, request));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.creation.validity_period_start_in_past", ex.getError().key());
        verify(accountRepository, never()).save(any());
        verify(accountStatusRepository, never()).save(any());
        verify(organizationalUnitAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should save account status with correct accountId, createdBy and updatedBy")
    void testCreate_shouldSaveStatusWithCorrectAuditFields() {
        UUID ouId = UUID.randomUUID();
        var request = new AccountRecord("ext-001", "Doe", "John", "john@example.com",
            new PeriodRecord(START, null), ouId);
        UUID generatedId = UUID.randomUUID();
        AccountStatus mockStatus = new AccountStatus();
        when(accountMapper.toAccount(eq(request), any(UserPrincipal.class))).thenReturn(new Account());
        when(checksumService.compute("{}")).thenReturn("fixed-checksum");
        when(accountRepository.save(any(Account.class)))
            .thenAnswer(invocation -> {
                Account saved = invocation.getArgument(0);
                saved.setId(generatedId);
                return saved;
            });
        when(accountStatusMapper.toAccountStatus(
            eq(request), any(UserPrincipal.class), argThat(a -> generatedId.equals(a.getId()))))
            .thenReturn(mockStatus);
        when(accountStatusRepository.save(any(AccountStatus.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(organizationalUnitAccountRepository.save(any(OrganizationalUnitAccount.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        accountService.create(userPrincipal, request);

        verify(accountStatusMapper).toAccountStatus(
            eq(request), any(UserPrincipal.class), argThat(a -> generatedId.equals(a.getId())));
        verify(accountStatusRepository).save(mockStatus);
    }

    @Test
    @DisplayName("create should validate first, then persist the account, then map and persist the status — in that order")
    void testCreate_shouldRespectValidatePersistAccountMapPersistStatusOrder() {
        UUID ouId = UUID.randomUUID();
        var request = new AccountRecord("ext-002", "Doe", "John", "john2@example.com",
            new PeriodRecord(START, null), ouId);
        AccountStatus mockStatus = new AccountStatus();
        when(accountMapper.toAccount(eq(request), any(UserPrincipal.class))).thenReturn(new Account());
        when(accountStatusMapper.toAccountStatus(
            any(AccountRecord.class), any(UserPrincipal.class), any(Account.class)))
            .thenReturn(mockStatus);
        when(checksumService.compute("{}")).thenReturn("fixed-checksum");
        when(accountRepository.save(any(Account.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(accountStatusRepository.save(any(AccountStatus.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(organizationalUnitAccountRepository.save(any(OrganizationalUnitAccount.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        accountService.create(userPrincipal, request);

        var inOrder = inOrder(accountCreationValidator, accountMapper, accountRepository,
            accountStatusMapper, accountStatusRepository);
        inOrder.verify(accountCreationValidator).validate(request);
        inOrder.verify(accountMapper).toAccount(eq(request), any(UserPrincipal.class));
        inOrder.verify(accountRepository).save(any(Account.class));
        inOrder.verify(accountStatusMapper).toAccountStatus(
            eq(request), any(UserPrincipal.class), any(Account.class));
        inOrder.verify(accountStatusRepository).save(mockStatus);
    }

    @Test
    @DisplayName("create should create organizational unit account link when organizationalUnitId is provided")
    void testCreate_shouldCreateOUAccountLinkWhenOUIdProvided() {
        UUID ouId = UUID.randomUUID();
        var request = new AccountRecord("ext-003", "Doe", "John", "john3@example.com",
            new PeriodRecord(START, null), ouId);
        UUID accountId = UUID.randomUUID();
        Account createdAccount = new Account();
        createdAccount.setId(accountId);
        AccountStatus mockStatus = new AccountStatus();

        when(accountMapper.toAccount(eq(request), any(UserPrincipal.class))).thenReturn(new Account());
        when(checksumService.compute("{}")).thenReturn("fixed-checksum");
        when(accountRepository.save(any(Account.class))).thenReturn(createdAccount);
        when(accountStatusMapper.toAccountStatus(
            any(AccountRecord.class), any(UserPrincipal.class), any(Account.class)))
            .thenReturn(mockStatus);
        when(accountStatusRepository.save(any(AccountStatus.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(organizationalUnitAccountRepository.save(any(OrganizationalUnitAccount.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        accountService.create(userPrincipal, request);

        verify(organizationalUnitAccountRepository).save(argThat(ouAccount ->
            ouAccount.getOrganizationalUnitId().equals(ouId) &&
                ouAccount.getAccountId().equals(accountId) &&
                ouAccount.getCreatedBy().equals(ADMIN_ID) &&
                ouAccount.getUpdatedBy().equals(ADMIN_ID)
        ));
    }

    @Test
    @DisplayName("create should respect order: validate, persist account, map status, persist status, then create OU link")
    void testCreate_shouldRespectOrderIncludingOULinkCreation() {
        UUID ouId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        var request = new AccountRecord("ext-006", "Doe", "John", "john6@example.com",
            new PeriodRecord(START, null), ouId);
        Account createdAccount = new Account();
        createdAccount.setId(accountId);
        AccountStatus mockStatus = new AccountStatus();

        when(accountMapper.toAccount(eq(request), any(UserPrincipal.class))).thenReturn(new Account());
        when(checksumService.compute("{}")).thenReturn("fixed-checksum");
        when(accountRepository.save(any(Account.class))).thenReturn(createdAccount);
        when(accountStatusMapper.toAccountStatus(
            any(AccountRecord.class), any(UserPrincipal.class), any(Account.class)))
            .thenReturn(mockStatus);
        when(accountStatusRepository.save(any(AccountStatus.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(organizationalUnitAccountRepository.save(any(OrganizationalUnitAccount.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        accountService.create(userPrincipal, request);

        var inOrder = inOrder(accountCreationValidator, accountRepository,
            accountStatusRepository, organizationalUnitAccountRepository);
        inOrder.verify(accountCreationValidator).validate(request);
        inOrder.verify(accountRepository).save(any(Account.class));
        inOrder.verify(accountStatusRepository).save(mockStatus);
        inOrder.verify(organizationalUnitAccountRepository).save(any(OrganizationalUnitAccount.class));
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
    @DisplayName("suspend should throw 404 when account not found")
    void testSuspend_shouldThrow404WhenAccountNotFound() {
        UUID id = UUID.randomUUID();
        var record = new AccountSuspensionRecord(
            new PeriodRecord(START, null), "REASON", "SUBREASON", "comment");

        when(accountRepository.existsById(id)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class,
            () -> accountService.suspend(userPrincipal, id, record));

        assertEquals(404, ex.getStatusCode());
        assertEquals("error.account.not_found", ex.getError().key());
        verify(accountSuspensionValidator, never()).validate(any(), any(), any());
        verify(accountStatusRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("suspend should throw 404 when account status row does not exist")
    void testSuspend_shouldThrow404WhenStatusNotFound() {
        UUID id = UUID.randomUUID();
        var record = new AccountSuspensionRecord(
            new PeriodRecord(START, null), "REASON", "SUBREASON", "comment");

        when(accountRepository.existsById(id)).thenReturn(true);
        when(accountStatusRepository.findByAccountId(id)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
            () -> accountService.suspend(userPrincipal, id, record));

        assertEquals(404, ex.getStatusCode());
        assertEquals("error.account.status.not_found", ex.getError().key());
        verify(accountSuspensionValidator, never()).validate(any(), any(), any());
        verify(accountStatusRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("suspend should propagate the exception thrown by the suspension validator")
    void testSuspend_shouldPropagateValidatorFailure() {
        UUID id = UUID.randomUUID();
        var existing = new AccountStatus();
        var record = new AccountSuspensionRecord(
            new PeriodRecord(START, null), "REASON", "SUBREASON", "comment");

        when(accountRepository.existsById(id)).thenReturn(true);
        when(accountStatusRepository.findByAccountId(id)).thenReturn(Optional.of(existing));
        doThrow(new ApiException(HttpStatus.BAD_REQUEST.value(),
            I18nMessage.of("error.account.status.suspension_start_in_past",
                Map.of("id", id.toString()))))
            .when(accountSuspensionValidator).validate(eq(existing), eq(record), eq(id));

        ApiException ex = assertThrows(ApiException.class,
            () -> accountService.suspend(userPrincipal, id, record));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.status.suspension_start_in_past", ex.getError().key());
        verify(accountStatusRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("suspend should apply suspension fields, save and return the refreshed view")
    void testSuspend_shouldApplyFieldsSaveAndReturnView() {
        UUID id = UUID.randomUUID();
        var view = new AccountView();
        view.setId(id);
        var existing = new AccountStatus();
        var record = new AccountSuspensionRecord(
            new PeriodRecord(START, null), "REASON", "SUBREASON", "comment");

        when(accountRepository.existsById(id)).thenReturn(true);
        when(accountStatusRepository.findByAccountId(id)).thenReturn(Optional.of(existing));
        when(accountStatusRepository.saveAndFlush(existing)).thenReturn(existing);
        when(accountViewRepository.findById(id)).thenReturn(Optional.of(view));

        AccountView result = accountService.suspend(userPrincipal, id, record);

        assertSame(view, result);
        assertEquals("REASON", existing.getSuspensionReason());
        assertEquals("SUBREASON", existing.getSuspensionSubreason());
        assertEquals("comment", existing.getSuspensionComment());
        assertEquals(ADMIN_ID, existing.getUpdatedBy());
        verify(accountSuspensionValidator).validate(existing, record, id);
        verify(accountStatusRepository).saveAndFlush(existing);
    }

    @Test
    @DisplayName("deactivate should throw 404 when account not found")
    void testDeactivate_shouldThrow404WhenAccountNotFound() {
        UUID id = UUID.randomUUID();
        var record = new AccountDeactivationRecord(START, "REASON", "SUBREASON", "comment");

        when(accountRepository.existsById(id)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class,
            () -> accountService.deactivate(userPrincipal, id, record));

        assertEquals(404, ex.getStatusCode());
        assertEquals("error.account.not_found", ex.getError().key());
        verify(accountDeactivationValidator, never()).validate(any(), any(), any());
        verify(accountStatusRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("deactivate should throw 404 when account status row does not exist")
    void testDeactivate_shouldThrow404WhenStatusNotFound() {
        UUID id = UUID.randomUUID();
        var record = new AccountDeactivationRecord(START, "REASON", "SUBREASON", "comment");

        when(accountRepository.existsById(id)).thenReturn(true);
        when(accountStatusRepository.findByAccountId(id)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
            () -> accountService.deactivate(userPrincipal, id, record));

        assertEquals(404, ex.getStatusCode());
        assertEquals("error.account.status.not_found", ex.getError().key());
        verify(accountDeactivationValidator, never()).validate(any(), any(), any());
        verify(accountStatusRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("deactivate should propagate the exception thrown by the deactivation validator")
    void testDeactivate_shouldPropagateValidatorFailure() {
        UUID id = UUID.randomUUID();
        var existing = new AccountStatus();
        var record = new AccountDeactivationRecord(START, "REASON", "SUBREASON", "comment");

        when(accountRepository.existsById(id)).thenReturn(true);
        when(accountStatusRepository.findByAccountId(id)).thenReturn(Optional.of(existing));
        doThrow(new ApiException(HttpStatus.BAD_REQUEST.value(),
            I18nMessage.of("error.account.status.deactivation_in_past",
                Map.of("id", id.toString()))))
            .when(accountDeactivationValidator).validate(eq(existing), eq(record), eq(id));

        ApiException ex = assertThrows(ApiException.class,
            () -> accountService.deactivate(userPrincipal, id, record));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.status.deactivation_in_past", ex.getError().key());
        verify(accountStatusRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("deactivate should set validity end to deactivationAt, save and return the refreshed view")
    void testDeactivate_shouldApplyFieldsSaveAndReturnView() {
        UUID id = UUID.randomUUID();
        var view = new AccountView();
        view.setId(id);
        var existing = new AccountStatus();
        existing.setValidityPeriod(commonMapper.toRange(new PeriodRecord(START, null)));
        OffsetDateTime deactivationAt = START.plusYears(1);
        var record = new AccountDeactivationRecord(deactivationAt, "REASON", "SUBREASON", "comment");

        when(accountRepository.existsById(id)).thenReturn(true);
        when(accountStatusRepository.findByAccountId(id)).thenReturn(Optional.of(existing));
        when(accountStatusRepository.saveAndFlush(existing)).thenReturn(existing);
        when(accountViewRepository.findById(id)).thenReturn(Optional.of(view));

        AccountView result = accountService.deactivate(userPrincipal, id, record);

        assertSame(view, result);
        assertEquals(START, commonMapper.startOf(existing.getValidityPeriod()));
        assertEquals(deactivationAt, commonMapper.endOf(existing.getValidityPeriod()));
        assertEquals("REASON", existing.getDeactivationReason());
        assertEquals("SUBREASON", existing.getDeactivationSubreason());
        assertEquals("comment", existing.getDeactivationComment());
        assertEquals(ADMIN_ID, existing.getUpdatedBy());
        verify(accountDeactivationValidator).validate(existing, record, id);
        verify(accountStatusRepository).saveAndFlush(existing);
    }

    @Test
    @DisplayName("reactivate should throw 404 when account not found")
    void testReactivate_shouldThrow404WhenAccountNotFound() {
        UUID id = UUID.randomUUID();
        var record = new AccountReactivationRecord("comment", null);

        when(accountRepository.existsById(id)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class,
            () -> accountService.reactivate(userPrincipal, id, record));

        assertEquals(404, ex.getStatusCode());
        assertEquals("error.account.not_found", ex.getError().key());
        verify(accountReactivationValidator, never()).validate(any(), any(), any());
        verify(accountStatusRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("reactivate should throw 404 when account status row does not exist")
    void testReactivate_shouldThrow404WhenStatusNotFound() {
        UUID id = UUID.randomUUID();
        var record = new AccountReactivationRecord("comment", null);

        when(accountRepository.existsById(id)).thenReturn(true);
        when(accountStatusRepository.findByAccountId(id)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
            () -> accountService.reactivate(userPrincipal, id, record));

        assertEquals(404, ex.getStatusCode());
        assertEquals("error.account.status.not_found", ex.getError().key());
        verify(accountReactivationValidator, never()).validate(any(), any(), any());
        verify(accountStatusRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("reactivate should propagate the exception thrown by the reactivation validator")
    void testReactivate_shouldPropagateValidatorFailure() {
        UUID id = UUID.randomUUID();
        var existing = new AccountStatus();
        var record = new AccountReactivationRecord("comment", null);

        when(accountRepository.existsById(id)).thenReturn(true);
        when(accountStatusRepository.findByAccountId(id)).thenReturn(Optional.of(existing));
        doThrow(new ApiException(HttpStatus.BAD_REQUEST.value(),
            I18nMessage.of("error.account.status.nothing_to_reactivate",
                Map.of("id", id.toString()))))
            .when(accountReactivationValidator).validate(eq(existing), eq(record), eq(id));

        ApiException ex = assertThrows(ApiException.class,
            () -> accountService.reactivate(userPrincipal, id, record));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.status.nothing_to_reactivate", ex.getError().key());
        verify(accountStatusRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("reactivate should close the suspension period, save and return the refreshed view")
    void testReactivate_shouldApplyFieldsSaveAndReturnView() {
        UUID id = UUID.randomUUID();
        var view = new AccountView();
        view.setId(id);
        var existing = new AccountStatus();
        OffsetDateTime suspensionStart = OffsetDateTime.now().minusDays(1);
        existing.setSuspensionPeriod(commonMapper.toRange(new PeriodRecord(suspensionStart, null)));
        var record = new AccountReactivationRecord("comment", null);

        when(accountRepository.existsById(id)).thenReturn(true);
        when(accountStatusRepository.findByAccountId(id)).thenReturn(Optional.of(existing));
        when(accountStatusRepository.saveAndFlush(existing)).thenReturn(existing);
        when(accountViewRepository.findById(id)).thenReturn(Optional.of(view));

        AccountView result = accountService.reactivate(userPrincipal, id, record);

        assertSame(view, result);
        assertEquals(suspensionStart.toInstant(),
            commonMapper.startOf(existing.getSuspensionPeriod()).toInstant());
        assertNotNull(commonMapper.endOf(existing.getSuspensionPeriod()));
        assertEquals("comment", existing.getReactivationComment());
        assertEquals(ADMIN_ID, existing.getUpdatedBy());
        verify(accountReactivationValidator).validate(existing, record, id);
        verify(accountStatusRepository).saveAndFlush(existing);
    }

    @Test
    @DisplayName("reactivate should push the validity end of a deactivated account and preserve its deactivation fields")
    void testReactivate_shouldPushValidityEndOfDeactivatedAccount() {
        UUID id = UUID.randomUUID();
        var view = new AccountView();
        view.setId(id);
        var existing = new AccountStatus();
        OffsetDateTime validityStart = OffsetDateTime.now().minusDays(30);
        existing.setValidityPeriod(commonMapper.toRange(
            new PeriodRecord(validityStart, OffsetDateTime.now().minusDays(1))));
        existing.setDeactivationReason("REASON");
        existing.setDeactivationSubreason("SUBREASON");
        existing.setDeactivationComment("deactivation comment");
        OffsetDateTime newEnd = OffsetDateTime.now().plusYears(1);
        var record = new AccountReactivationRecord("comment", newEnd);

        when(accountRepository.existsById(id)).thenReturn(true);
        when(accountStatusRepository.findByAccountId(id)).thenReturn(Optional.of(existing));
        when(accountStatusRepository.saveAndFlush(existing)).thenReturn(existing);
        when(accountViewRepository.findById(id)).thenReturn(Optional.of(view));

        AccountView result = accountService.reactivate(userPrincipal, id, record);

        assertSame(view, result);
        assertEquals(validityStart.toInstant(),
            commonMapper.startOf(existing.getValidityPeriod()).toInstant());
        assertEquals(newEnd.toInstant(),
            commonMapper.endOf(existing.getValidityPeriod()).toInstant());
        assertEquals("comment", existing.getReactivationComment());
        assertEquals("REASON", existing.getDeactivationReason());
        assertEquals("SUBREASON", existing.getDeactivationSubreason());
        assertEquals("deactivation comment", existing.getDeactivationComment());
        assertEquals(ADMIN_ID, existing.getUpdatedBy());
        verify(accountReactivationValidator).validate(existing, record, id);
        verify(accountStatusRepository).saveAndFlush(existing);
    }

    @Test
    @DisplayName("updateValidity should throw 404 when account not found")
    void testUpdateValidity_shouldThrow404WhenAccountNotFound() {
        UUID id = UUID.randomUUID();
        var record = new AccountValidityRecord(OffsetDateTime.now().plusDays(10));

        when(accountRepository.existsById(id)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class,
            () -> accountService.updateValidity(userPrincipal, id, record));

        assertEquals(404, ex.getStatusCode());
        assertEquals("error.account.not_found", ex.getError().key());
        verify(accountValidityValidator, never()).validate(any(), any(), any());
        verify(accountStatusRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("updateValidity should throw 404 when account status row does not exist")
    void testUpdateValidity_shouldThrow404WhenStatusNotFound() {
        UUID id = UUID.randomUUID();
        var record = new AccountValidityRecord(OffsetDateTime.now().plusDays(10));

        when(accountRepository.existsById(id)).thenReturn(true);
        when(accountStatusRepository.findByAccountId(id)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
            () -> accountService.updateValidity(userPrincipal, id, record));

        assertEquals(404, ex.getStatusCode());
        assertEquals("error.account.status.not_found", ex.getError().key());
        verify(accountValidityValidator, never()).validate(any(), any(), any());
        verify(accountStatusRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("updateValidity should set the validity start, preserve the end, save and return the refreshed view")
    void testUpdateValidity_shouldApplyFieldsSaveAndReturnView() {
        UUID id = UUID.randomUUID();
        var view = new AccountView();
        view.setId(id);
        var existing = new AccountStatus();
        OffsetDateTime existingEnd = START.plusYears(2);
        existing.setValidityPeriod(commonMapper.toRange(new PeriodRecord(START, existingEnd)));
        OffsetDateTime newStart = START.plusDays(10);
        var record = new AccountValidityRecord(newStart);

        when(accountRepository.existsById(id)).thenReturn(true);
        when(accountStatusRepository.findByAccountId(id)).thenReturn(Optional.of(existing));
        when(accountStatusRepository.saveAndFlush(existing)).thenReturn(existing);
        when(accountViewRepository.findById(id)).thenReturn(Optional.of(view));

        AccountView result = accountService.updateValidity(userPrincipal, id, record);

        assertSame(view, result);
        assertEquals(newStart, commonMapper.startOf(existing.getValidityPeriod()));
        assertEquals(existingEnd, commonMapper.endOf(existing.getValidityPeriod()));
        assertEquals(ADMIN_ID, existing.getUpdatedBy());
        verify(accountValidityValidator).validate(existing, record, id);
        verify(accountStatusRepository).saveAndFlush(existing);
    }

    @Test
    @DisplayName("updateValidity should propagate the exception thrown by the validity validator")
    void testUpdateValidity_shouldPropagateValidatorFailure() {
        UUID id = UUID.randomUUID();
        var existing = new AccountStatus();
        var record = new AccountValidityRecord(OffsetDateTime.now().plusDays(10));

        when(accountRepository.existsById(id)).thenReturn(true);
        when(accountStatusRepository.findByAccountId(id)).thenReturn(Optional.of(existing));
        doThrow(new ApiException(HttpStatus.BAD_REQUEST.value(),
            I18nMessage.of("error.account.status.validity_start_not_in_future",
                Map.of("id", id.toString()))))
            .when(accountValidityValidator).validate(eq(existing), eq(record), eq(id));

        ApiException ex = assertThrows(ApiException.class,
            () -> accountService.updateValidity(userPrincipal, id, record));

        assertEquals(400, ex.getStatusCode());
        assertEquals("error.account.status.validity_start_not_in_future", ex.getError().key());
        verify(accountStatusRepository, never()).saveAndFlush(any());
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
