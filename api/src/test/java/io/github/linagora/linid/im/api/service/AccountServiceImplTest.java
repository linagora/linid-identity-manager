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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.linagora.linid.im.api.model.account.AccountRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.Account;
import io.github.linagora.linid.im.api.persistence.model.AccountQueryFilterDto;
import io.github.linagora.linid.im.api.persistence.repository.AccountRepository;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: AccountServiceImpl")
class AccountServiceImplTest {

  @Mock
  private AccountRepository accountRepository;

  @Mock
  private ChecksumService checksumService;

  @InjectMocks
  private AccountServiceImpl accountService;

  private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

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
  @SuppressWarnings("unchecked")
  void testFindAll_shouldDelegateToRepository() {
    var pageable = PageRequest.of(0, 10);
    var entity = new Account();
    var filters = new AccountQueryFilterDto();
    when(accountRepository.findAll(any(Specification.class), any(PageRequest.class)))
        .thenReturn(new PageImpl<>(List.of(entity)));

    Page<Account> result = accountService.findAll(userPrincipal, filters, pageable);

    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    verify(accountRepository).findAll(any(Specification.class), any(PageRequest.class));
  }

  @Test
  @DisplayName("Should return account when found by ID")
  void testFindById_shouldReturnAccountWhenFound() {
    UUID id = UUID.randomUUID();
    var entity = new Account();
    entity.setId(id);
    when(accountRepository.findById(id)).thenReturn(Optional.of(entity));

    Account result = accountService.findById(userPrincipal, id);

    assertNotNull(result);
    assertEquals(id, result.getId());
  }

  @Test
  @DisplayName("Should throw ApiException 404 when account not found")
  void testFindById_shouldThrow404WhenNotFound() {
    UUID id = UUID.randomUUID();
    when(accountRepository.findById(id)).thenReturn(Optional.empty());

    ApiException exception = assertThrows(ApiException.class,
        () -> accountService.findById(userPrincipal, id));

    assertEquals(404, exception.getStatusCode());
    assertEquals("error.account.not_found", exception.getError().key());
  }

  @Test
  @DisplayName("Should delete account when it exists")
  void testDeleteById_shouldDeleteWhenFound() {
    UUID id = UUID.randomUUID();
    var entity = new Account();
    entity.setId(id);
    when(accountRepository.findById(id)).thenReturn(Optional.of(entity));

    accountService.deleteById(userPrincipal, id);

    verify(accountRepository).deleteById(id);
  }

  @Test
  @DisplayName("Should throw ApiException 404 when deleting non-existent account")
  void testDeleteById_shouldThrow404WhenNotFound() {
    UUID id = UUID.randomUUID();
    when(accountRepository.findById(id)).thenReturn(Optional.empty());

    ApiException exception = assertThrows(ApiException.class,
        () -> accountService.deleteById(userPrincipal, id));

    assertEquals(404, exception.getStatusCode());
    assertEquals("error.account.not_found", exception.getError().key());
    verify(accountRepository, never()).deleteById(any());
  }
}
