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

package io.github.linagora.linid.im.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.linagora.linid.im.api.model.account.AccountActivationRecord;
import io.github.linagora.linid.im.api.model.account.AccountDTO;
import io.github.linagora.linid.im.api.model.account.AccountDeactivationRecord;
import io.github.linagora.linid.im.api.model.account.AccountMapper;
import io.github.linagora.linid.im.api.model.account.AccountReactivationRecord;
import io.github.linagora.linid.im.api.model.account.AccountRecord;
import io.github.linagora.linid.im.api.model.account.AccountSuspensionRecord;
import io.github.linagora.linid.im.api.model.account.AccountValidityRecord;
import io.github.linagora.linid.im.api.model.account.AccountViewDTO;
import io.github.linagora.linid.im.api.model.common.PeriodRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.Account;
import io.github.linagora.linid.im.api.persistence.model.AccountView;
import io.github.linagora.linid.im.api.persistence.model.AccountViewQueryFilterDto;
import io.github.linagora.linid.im.api.service.AccountService;
import io.github.linagora.linid.im.api.service.OrganizationalUnitService;
import java.time.OffsetDateTime;
import java.util.List;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: AccountController")
class AccountControllerTest {

    @Mock
    private AccountService accountService;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private PagedResponseStatusResolver pagedResponseStatusResolver;

    @Mock
    private OrganizationalUnitService organizationalUnitService;

    @InjectMocks
    private AccountController accountController;

    private UserPrincipal userPrincipal;

    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final String ADMIN_FULL_NAME = "Admin User";

    private static final OffsetDateTime START = OffsetDateTime.parse("2100-01-01T00:00:00Z");

    @BeforeEach
    void setUp() {
        userPrincipal = new UserPrincipal();
        userPrincipal.setId(ADMIN_ID);
        userPrincipal.setEmail("admin@example.com");
    }

    private Account createSampleEntity() {
        Account entity = new Account();
        entity.setId(UUID.randomUUID());
        entity.setExternalId("ext-001");
        entity.setLastname("Doe");
        entity.setFirstname("John");
        entity.setEmail("john@example.com");
        entity.setCreatedBy(ADMIN_ID);
        entity.setUpdatedBy(ADMIN_ID);
        entity.setInsertDate(OffsetDateTime.now());
        entity.setUpdateDate(OffsetDateTime.now());
        return entity;
    }

    private AccountDTO createSampleDTO(final Account entity) {
        return AccountDTO.builder()
            .id(entity.getId())
            .externalId(entity.getExternalId())
            .lastname(entity.getLastname())
            .firstname(entity.getFirstname())
            .email(entity.getEmail())
            .createdBy(entity.getCreatedBy())
            .updatedBy(entity.getUpdatedBy())
            .insertDate(entity.getInsertDate())
            .updateDate(entity.getUpdateDate())
            .build();
    }

    private AccountView createSampleViewEntity() {
        Account entity = new Account();
        return AccountView.builder()
            .id(entity.getId())
            .externalId(entity.getExternalId())
            .lastname(entity.getLastname())
            .firstname(entity.getFirstname())
            .email(entity.getEmail())
            .createdBy(ADMIN_FULL_NAME)
            .updatedBy(ADMIN_FULL_NAME)
            .insertDate(entity.getInsertDate())
            .updateDate(entity.getUpdateDate())
            .build();
    }

    private AccountViewDTO createSampleViewDTO(final AccountView viewEntity) {
        return AccountViewDTO.builder()
            .id(viewEntity.getId())
            .externalId(viewEntity.getExternalId())
            .lastname(viewEntity.getLastname())
            .firstname(viewEntity.getFirstname())
            .email(viewEntity.getEmail())
            .createdBy(viewEntity.getCreatedBy())
            .updatedBy(viewEntity.getUpdatedBy())
            .insertDate(viewEntity.getInsertDate())
            .updateDate(viewEntity.getUpdateDate())
            .build();
    }

    @Test
    @DisplayName("Should create account and return 201")
    void testCreate_shouldReturn201WithAccountDTO() {
        UUID ouId = UUID.randomUUID();
        var request = new AccountRecord("ext-001", "Doe", "John", "john@example.com", new PeriodRecord(START, null), ouId);
        var entity = createSampleEntity();
        var dto = createSampleDTO(entity);
        when(accountService.create(userPrincipal, request)).thenReturn(entity);
        when(accountMapper.toDTO(entity)).thenReturn(dto);

        ResponseEntity<AccountDTO> response = accountController.create(userPrincipal, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(entity.getId(), response.getBody().getId());
        assertEquals("ext-001", response.getBody().getExternalId());
        assertEquals("Doe", response.getBody().getLastname());
        assertEquals("John", response.getBody().getFirstname());
        assertEquals("john@example.com", response.getBody().getEmail());
        assertEquals(ADMIN_ID, response.getBody().getCreatedBy());
        assertEquals(ADMIN_ID, response.getBody().getUpdatedBy());
        assertNotNull(response.getBody().getInsertDate());
        assertNotNull(response.getBody().getUpdateDate());
        var inOrder = inOrder(organizationalUnitService, accountService);
        inOrder.verify(organizationalUnitService).existsById(userPrincipal, ouId);
        inOrder.verify(accountService).create(userPrincipal, request);
    }

    @Test
    @DisplayName("Should return paginated accounts with mapped DTOs")
    @SuppressWarnings("unchecked")
    void testFindAll_shouldReturnPaginatedAccountDTOs() {
        var entity = createSampleViewEntity();
        var pageable = PageRequest.of(0, 10);
        var filters = new AccountViewQueryFilterDto();
        when(
            accountService.findAll(any(UserPrincipal.class), any(AccountViewQueryFilterDto.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(entity)));
        when(accountMapper.toDTO(entity)).thenReturn(createSampleViewDTO(entity));
        when(pagedResponseStatusResolver.resolve(any(Page.class)))
            .thenAnswer(invocation -> ResponseEntity.ok(invocation.getArgument(0)));

        ResponseEntity<Page<AccountViewDTO>> response =
            accountController.findAll(userPrincipal, filters, pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
    }

    @Test
    @DisplayName("Should return account by ID with 200")
    void testFindById_shouldReturn200WithAccount() {
        var entity = createSampleViewEntity();
        var dto = createSampleViewDTO(entity);
        when(accountService.findById(userPrincipal, entity.getId())).thenReturn(entity);
        when(accountMapper.toDTO(entity)).thenReturn(dto);

        ResponseEntity<AccountViewDTO> response = accountController.findById(userPrincipal, entity.getId());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(entity.getId(), response.getBody().getId());
    }

    @Test
    @DisplayName("Should delete account and return 204")
    void testDeleteById_shouldReturn204() {
        UUID id = UUID.randomUUID();

        ResponseEntity<Void> response = accountController.deleteById(userPrincipal, id);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(accountService).deleteById(userPrincipal, id);
    }

    @Test
    @DisplayName("Should suspend account and return 200 with AccountViewDTO")
    void testSuspend_shouldReturn200WithAccountViewDTO() {
        UUID id = UUID.randomUUID();
        var record = new AccountSuspensionRecord(new PeriodRecord(START, null), "REASON", null, null);
        var entity = createSampleViewEntity();
        var dto = createSampleViewDTO(entity);
        when(accountService.suspend(userPrincipal, id, record)).thenReturn(entity);
        when(accountMapper.toDTO(entity)).thenReturn(dto);

        ResponseEntity<AccountViewDTO> response = accountController.suspend(userPrincipal, id, record);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(entity.getId(), response.getBody().getId());
        verify(accountService).suspend(userPrincipal, id, record);
    }

    @Test
    @DisplayName("Should deactivate account and return 200 with AccountViewDTO")
    void testDeactivate_shouldReturn200WithAccountViewDTO() {
        UUID id = UUID.randomUUID();
        var record = new AccountDeactivationRecord(START, "REASON", null, null);
        var entity = createSampleViewEntity();
        var dto = createSampleViewDTO(entity);
        when(accountService.deactivate(userPrincipal, id, record)).thenReturn(entity);
        when(accountMapper.toDTO(entity)).thenReturn(dto);

        ResponseEntity<AccountViewDTO> response = accountController.deactivate(userPrincipal, id, record);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(entity.getId(), response.getBody().getId());
        verify(accountService).deactivate(userPrincipal, id, record);
    }

    @Test
    @DisplayName("Should reactivate account and return 200 with AccountViewDTO")
    void testReactivate_shouldReturn200WithAccountViewDTO() {
        UUID id = UUID.randomUUID();
        var record = new AccountReactivationRecord("Investigation closed", null);
        var entity = createSampleViewEntity();
        var dto = createSampleViewDTO(entity);
        when(accountService.reactivate(userPrincipal, id, record)).thenReturn(entity);
        when(accountMapper.toDTO(entity)).thenReturn(dto);

        ResponseEntity<AccountViewDTO> response = accountController.reactivate(userPrincipal, id, record);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(entity.getId(), response.getBody().getId());
        verify(accountService).reactivate(userPrincipal, id, record);
    }

    @Test
    @DisplayName("Should activate account and return 200 with AccountViewDTO")
    void testActivate_shouldReturn200WithAccountViewDTO() {
        UUID id = UUID.randomUUID();
        var record = new AccountActivationRecord(OffsetDateTime.now());
        var entity = createSampleViewEntity();
        var dto = createSampleViewDTO(entity);
        when(accountService.updateActivation(userPrincipal, id, record)).thenReturn(entity);
        when(accountMapper.toDTO(entity)).thenReturn(dto);

        ResponseEntity<AccountViewDTO> response = accountController.activate(userPrincipal, id, record);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(entity.getId(), response.getBody().getId());
    }

    @Test
    @DisplayName("Should schedule activation and return 200 with AccountViewDTO")
    void testScheduleActivation_shouldReturn200WithAccountViewDTO() {
        UUID id = UUID.randomUUID();
        var record = new AccountValidityRecord(START);
        var entity = createSampleViewEntity();
        var dto = createSampleViewDTO(entity);
        when(accountService.updateValidity(userPrincipal, id, record)).thenReturn(entity);
        when(accountMapper.toDTO(entity)).thenReturn(dto);

        ResponseEntity<AccountViewDTO> response = accountController.scheduleActivation(userPrincipal, id, record);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(entity.getId(), response.getBody().getId());
        verify(accountService).updateValidity(userPrincipal, id, record);
    }
}
