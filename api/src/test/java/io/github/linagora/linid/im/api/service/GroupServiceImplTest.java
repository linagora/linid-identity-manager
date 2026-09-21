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

import io.github.linagora.linid.im.api.model.group.GroupAccountRecord;
import io.github.linagora.linid.im.api.model.group.GroupMapper;
import io.github.linagora.linid.im.api.model.group.GroupRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.Group;
import io.github.linagora.linid.im.api.persistence.model.GroupAccount;
import io.github.linagora.linid.im.api.persistence.model.GroupAccountView;
import io.github.linagora.linid.im.api.persistence.model.GroupAccountViewQueryFilterDto;
import io.github.linagora.linid.im.api.persistence.model.GroupAncestor;
import io.github.linagora.linid.im.api.persistence.model.GroupView;
import io.github.linagora.linid.im.api.persistence.model.GroupViewQueryFilterDto;
import io.github.linagora.linid.im.api.persistence.repository.AccountRepository;
import io.github.linagora.linid.im.api.persistence.repository.GroupAccountRepository;
import io.github.linagora.linid.im.api.persistence.repository.GroupAccountViewRepository;
import io.github.linagora.linid.im.api.persistence.repository.GroupAncestorRepository;
import io.github.linagora.linid.im.api.persistence.repository.GroupRepository;
import io.github.linagora.linid.im.api.persistence.repository.GroupViewRepository;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: GroupServiceImpl")
class GroupServiceImplTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupAncestorRepository groupAncestorRepository;

    @Mock
    private GroupViewRepository groupViewRepository;

    @Mock
    private GroupAccountRepository groupAccountRepository;

    @Mock
    private GroupAccountViewRepository groupAccountViewRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private OrganizationalUnitService organizationalUnitService;

    @Mock
    private ApplicationService applicationService;

    @Mock
    private GroupMapper mapper;

    @Mock
    private AvatarService avatarService;

    @InjectMocks
    private GroupServiceImpl service;

    private UserPrincipal userPrincipal;

    private GroupRecord record;

    @BeforeEach
    void setUp() {
        userPrincipal = new UserPrincipal();
        userPrincipal.setId(UUID.randomUUID());
        userPrincipal.setEmail("admin@example.com");
        record = new GroupRecord("developers", "Developers", null, "desc", "developers@example.com", null, null,
            Map.of());
    }

    @Test
    @DisplayName("create should persist the mapped group without checking absent references")
    void testCreate() {
        var mapped = Group.builder().code("developers").build();
        when(groupRepository.existsByCode("developers")).thenReturn(false);
        when(mapper.toEntity(record, userPrincipal)).thenReturn(mapped);
        when(groupRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(userPrincipal, record);

        var captor = ArgumentCaptor.forClass(Group.class);
        verify(groupRepository).save(captor.capture());

        // The mapped entity must be the one persisted, unchanged.
        assertSame(mapped, captor.getValue());
        assertSame(mapped, result);
        // No reference is provided: none must be looked up.
        verify(groupRepository, never()).findById(any());
        verify(organizationalUnitService, never()).existsById(any(), any());
        verify(applicationService, never()).findById(any(), any());
    }

    @Test
    @DisplayName("create should persist the group when every provided reference exists")
    void testCreate_withExistingReferences() {
        var parentId = UUID.randomUUID();
        var organizationalUnitId = UUID.randomUUID();
        var applicationId = UUID.randomUUID();
        var withReferences = new GroupRecord("developers", "Developers", parentId, null, null,
            organizationalUnitId, applicationId, Map.of());
        var mapped = Group.builder().code("developers").parentId(parentId).build();
        when(groupRepository.existsByCode("developers")).thenReturn(false);
        when(groupRepository.existsById(parentId)).thenReturn(true);
        when(mapper.toEntity(withReferences, userPrincipal)).thenReturn(mapped);
        when(groupRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(userPrincipal, withReferences);

        assertSame(mapped, result);
        verify(groupAncestorRepository, never()).findAllByGroupId(any());
        verify(organizationalUnitService).existsById(userPrincipal, organizationalUnitId);
        verify(applicationService).findById(userPrincipal, applicationId);
    }

    @Test
    @DisplayName("create should throw when the code already exists")
    void testCreate_shouldThrowOnDuplicateCode() {
        when(groupRepository.existsByCode("developers")).thenReturn(true);

        var exception = assertThrows(ApiException.class, () -> service.create(userPrincipal, record));
        assertEquals(400, exception.getStatusCode());
        assertEquals("error.group.code.already_exists", exception.getError().key());
        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("create should throw when the parent group does not exist")
    void testCreate_shouldThrowWhenParentAbsent() {
        var parentId = UUID.randomUUID();
        var withParent = new GroupRecord("developers", "Developers", parentId, null, null, null, null, Map.of());
        when(groupRepository.existsByCode("developers")).thenReturn(false);
        when(groupRepository.existsById(parentId)).thenReturn(false);

        var exception = assertThrows(ApiException.class, () -> service.create(userPrincipal, withParent));
        assertEquals(404, exception.getStatusCode());
        assertEquals("error.group.not_found", exception.getError().key());
        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("create should propagate the organizational unit service error when the unit does not exist")
    void testCreate_shouldThrowWhenOrganizationalUnitAbsent() {
        var organizationalUnitId = UUID.randomUUID();
        var withOrganizationalUnit = new GroupRecord("developers", "Developers", null, null, null,
            organizationalUnitId, null, Map.of());
        when(groupRepository.existsByCode("developers")).thenReturn(false);
        doThrow(new ApiException(404, I18nMessage.of("error.organizational.unit.not_found")))
            .when(organizationalUnitService).existsById(userPrincipal, organizationalUnitId);

        var exception = assertThrows(ApiException.class,
            () -> service.create(userPrincipal, withOrganizationalUnit));
        assertEquals(404, exception.getStatusCode());
        assertEquals("error.organizational.unit.not_found", exception.getError().key());
        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("create should propagate the application service error when the application does not exist")
    void testCreate_shouldThrowWhenApplicationAbsent() {
        var applicationId = UUID.randomUUID();
        var withApplication = new GroupRecord("developers", "Developers", null, null, null, null, applicationId,
            Map.of());
        when(groupRepository.existsByCode("developers")).thenReturn(false);
        when(applicationService.findById(userPrincipal, applicationId))
            .thenThrow(new ApiException(404, I18nMessage.of("error.application.not_found")));

        var exception = assertThrows(ApiException.class, () -> service.create(userPrincipal, withApplication));
        assertEquals(404, exception.getStatusCode());
        assertEquals("error.application.not_found", exception.getError().key());
        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("findById should throw when the group does not exist")
    void testFindById_shouldThrowWhenAbsent() {
        var id = UUID.randomUUID();
        when(groupRepository.findById(id)).thenReturn(Optional.empty());

        var exception = assertThrows(ApiException.class, () -> service.findById(userPrincipal, id));
        assertEquals(404, exception.getStatusCode());
        assertEquals("error.group.not_found", exception.getError().key());
    }

    @Test
    @DisplayName("findViewById should throw when the group does not exist")
    void testFindViewById_shouldThrowWhenAbsent() {
        var id = UUID.randomUUID();
        when(groupViewRepository.findById(id)).thenReturn(Optional.empty());

        var exception = assertThrows(ApiException.class, () -> service.findViewById(userPrincipal, id));
        assertEquals(404, exception.getStatusCode());
        assertEquals("error.group.not_found", exception.getError().key());
    }

    @Test
    @DisplayName("findAll should query the view repository with a specification")
    void testFindAll() {
        when(groupViewRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(new GroupView())));

        var page = service.findAll(userPrincipal, new GroupViewQueryFilterDto(), Pageable.unpaged());

        assertEquals(1, page.getTotalElements());
    }

    @Test
    @DisplayName("update should apply record fields and persist the same entity")
    void testUpdate() {
        var id = UUID.randomUUID();
        var parentId = UUID.randomUUID();
        var organizationalUnitId = UUID.randomUUID();
        var applicationId = UUID.randomUUID();
        var update = new GroupRecord("developers", "Developers", parentId, "desc", "developers@example.com",
            organizationalUnitId, applicationId, Map.of("team", "core"));
        var existing = Group.builder().id(id).code("old-code").name("Old name").build();
        when(groupRepository.findById(id)).thenReturn(Optional.of(existing));
        when(groupRepository.existsByCodeAndIdNot("developers", id)).thenReturn(false);
        when(groupRepository.existsById(parentId)).thenReturn(true);
        when(groupAncestorRepository.findAllByGroupId(parentId)).thenReturn(List.of());
        when(groupRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.update(userPrincipal, id, update);

        var captor = ArgumentCaptor.forClass(Group.class);
        verify(groupRepository).save(captor.capture());
        var saved = captor.getValue();

        // The persisted entity must be the loaded one, with every record field propagated.
        assertSame(existing, saved);
        assertSame(saved, result);
        assertEquals(id, saved.getId());
        assertEquals("developers", saved.getCode());
        assertEquals("Developers", saved.getName());
        assertEquals(parentId, saved.getParentId());
        assertEquals("desc", saved.getDescription());
        assertEquals("developers@example.com", saved.getEmail());
        assertEquals(organizationalUnitId, saved.getOrganizationalUnitId());
        assertEquals(applicationId, saved.getApplicationId());
        assertEquals(Map.of("team", "core"), saved.getExtraParameters());
        assertEquals(userPrincipal.getId(), saved.getUpdatedBy());
        verify(organizationalUnitService).existsById(userPrincipal, organizationalUnitId);
        verify(applicationService).findById(userPrincipal, applicationId);
    }

    @Test
    @DisplayName("update should throw when another group already uses the code")
    void testUpdate_shouldThrowOnDuplicateCode() {
        var id = UUID.randomUUID();
        var existing = Group.builder().id(id).code("old-code").build();
        when(groupRepository.findById(id)).thenReturn(Optional.of(existing));
        when(groupRepository.existsByCodeAndIdNot("developers", id)).thenReturn(true);

        var exception = assertThrows(ApiException.class, () -> service.update(userPrincipal, id, record));
        assertEquals(400, exception.getStatusCode());
        assertEquals("error.group.code.already_exists", exception.getError().key());
        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("update should throw when the parent group does not exist")
    void testUpdate_shouldThrowWhenParentAbsent() {
        var id = UUID.randomUUID();
        var parentId = UUID.randomUUID();
        var withParent = new GroupRecord("developers", "Developers", parentId, null, null, null, null, Map.of());
        var existing = Group.builder().id(id).code("developers").build();
        when(groupRepository.findById(id)).thenReturn(Optional.of(existing));
        when(groupRepository.existsByCodeAndIdNot("developers", id)).thenReturn(false);
        when(groupRepository.existsById(parentId)).thenReturn(false);

        var exception = assertThrows(ApiException.class, () -> service.update(userPrincipal, id, withParent));
        assertEquals(404, exception.getStatusCode());
        assertEquals("error.group.not_found", exception.getError().key());
        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("update should throw when the group is set as its own parent")
    void testUpdate_shouldThrowWhenParentIsSelf() {
        var id = UUID.randomUUID();
        var selfParent = new GroupRecord("developers", "Developers", id, null, null, null, null, Map.of());
        var existing = Group.builder().id(id).code("developers").build();
        when(groupRepository.findById(id)).thenReturn(Optional.of(existing));
        when(groupRepository.existsByCodeAndIdNot("developers", id)).thenReturn(false);
        when(groupRepository.existsById(id)).thenReturn(true);

        var exception = assertThrows(ApiException.class, () -> service.update(userPrincipal, id, selfParent));
        assertEquals(400, exception.getStatusCode());
        assertEquals("error.group.parent.cycle", exception.getError().key());
        verify(groupAncestorRepository, never()).findAllByGroupId(any());
        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("update should throw when the candidate parent is a descendant of the group")
    void testUpdate_shouldThrowWhenParentIsDescendant() {
        var id = UUID.randomUUID();
        var grandChildId = UUID.randomUUID();
        var descendantParent = new GroupRecord("developers", "Developers", grandChildId, null, null, null, null,
            Map.of());
        var existing = Group.builder().id(id).code("developers").build();
        when(groupRepository.findById(id)).thenReturn(Optional.of(existing));
        when(groupRepository.existsByCodeAndIdNot("developers", id)).thenReturn(false);
        when(groupRepository.existsById(grandChildId)).thenReturn(true);
        // The group is an ancestor of grandChild: attaching it under grandChild closes the loop.
        when(groupAncestorRepository.findAllByGroupId(grandChildId))
            .thenReturn(List.of(new GroupAncestor(id.toString() + grandChildId, grandChildId, id)));

        var exception = assertThrows(ApiException.class,
            () -> service.update(userPrincipal, id, descendantParent));
        assertEquals(400, exception.getStatusCode());
        assertEquals("error.group.parent.cycle", exception.getError().key());
        verify(groupRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteById should delete an existing group")
    void testDeleteById() {
        var id = UUID.randomUUID();
        var existing = Group.builder().id(id).code("developers").build();
        when(groupRepository.findById(id)).thenReturn(Optional.of(existing));

        service.deleteById(userPrincipal, id);

        var captor = ArgumentCaptor.forClass(Group.class);
        verify(groupRepository).delete(captor.capture());
        assertSame(existing, captor.getValue());
    }

    @Test
    @DisplayName("deleteById should throw when the group does not exist")
    void testDeleteById_shouldThrowWhenAbsent() {
        var id = UUID.randomUUID();
        when(groupRepository.findById(id)).thenReturn(Optional.empty());

        var exception = assertThrows(ApiException.class, () -> service.deleteById(userPrincipal, id));
        assertEquals(404, exception.getStatusCode());
        assertEquals("error.group.not_found", exception.getError().key());
        verify(groupRepository, never()).delete(any(Group.class));
    }

    @Test
    @DisplayName("update should keep the stored extra parameters when the record omits them")
    void testUpdate_shouldKeepExtraParametersWhenOmitted() {
        var id = UUID.randomUUID();
        var withoutExtraParameters = new GroupRecord("developers", "Developers", null, null, null, null, null, null);
        var existing = Group.builder().id(id).code("developers").extraParameters(Map.of("team", "core")).build();
        when(groupRepository.findById(id)).thenReturn(Optional.of(existing));
        when(groupRepository.existsByCodeAndIdNot("developers", id)).thenReturn(false);
        when(groupRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.update(userPrincipal, id, withoutExtraParameters);

        assertEquals(Map.of("team", "core"), result.getExtraParameters());
    }

    @Test
    @DisplayName("existsById should not throw when the group exists")
    void testExistsById_shouldNotThrowWhenPresent() {
        var id = UUID.randomUUID();
        when(groupRepository.existsById(id)).thenReturn(true);

        service.existsById(userPrincipal, id);
    }

    @Test
    @DisplayName("existsById should throw a 404 when the group does not exist")
    void testExistsById_shouldThrowWhenAbsent() {
        var id = UUID.randomUUID();
        when(groupRepository.existsById(id)).thenReturn(false);

        var exception = assertThrows(ApiException.class, () -> service.existsById(userPrincipal, id));

        assertEquals(404, exception.getStatusCode());
        assertEquals("error.group.not_found", exception.getError().key());
    }

    @Test
    @DisplayName("findAllAccounts should delegate to the group account view repository")
    void testFindAllAccounts_shouldDelegateToRepository() {
        var entity = GroupAccountView.builder().id(UUID.randomUUID()).groupId(UUID.randomUUID()).build();
        var filters = new GroupAccountViewQueryFilterDto();
        when(groupAccountViewRepository.findAll(
            ArgumentMatchers.<Specification<GroupAccountView>>any(),
            ArgumentMatchers.any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(entity)));

        var result = service.findAllAccounts(userPrincipal, filters, Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
        assertEquals(entity.getId(), result.getContent().getFirst().getId());
    }

    @Test
    @DisplayName("attachAccount should persist the relationship with the default extra parameters")
    void testAttachAccount_shouldPersistRelationship() {
        var groupId = UUID.randomUUID();
        var accountId = UUID.randomUUID();
        when(groupRepository.existsById(groupId)).thenReturn(true);
        when(accountRepository.existsById(accountId)).thenReturn(true);
        when(groupAccountRepository.existsByGroupIdAndAccountId(groupId, accountId)).thenReturn(false);
        when(groupAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.attachAccount(userPrincipal, groupId, new GroupAccountRecord(accountId, null));

        assertEquals(groupId, result.getGroupId());
        assertEquals(accountId, result.getAccountId());
        assertEquals(Map.of(), result.getExtraParameters());
        assertEquals(userPrincipal.getId(), result.getCreatedBy());
        assertEquals(userPrincipal.getId(), result.getUpdatedBy());
    }

    @Test
    @DisplayName("attachAccount should throw a 404 when the group does not exist")
    void testAttachAccount_shouldThrowWhenGroupAbsent() {
        var groupId = UUID.randomUUID();
        when(groupRepository.existsById(groupId)).thenReturn(false);

        var exception = assertThrows(ApiException.class, () -> service.attachAccount(
            userPrincipal, groupId, new GroupAccountRecord(UUID.randomUUID(), Map.of())));

        assertEquals(404, exception.getStatusCode());
        assertEquals("error.group.not_found", exception.getError().key());
        verify(groupAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("attachAccount should throw a 404 when the account does not exist")
    void testAttachAccount_shouldThrowWhenAccountAbsent() {
        var groupId = UUID.randomUUID();
        var accountId = UUID.randomUUID();
        when(groupRepository.existsById(groupId)).thenReturn(true);
        when(accountRepository.existsById(accountId)).thenReturn(false);

        var exception = assertThrows(ApiException.class, () -> service.attachAccount(
            userPrincipal, groupId, new GroupAccountRecord(accountId, Map.of())));

        assertEquals(404, exception.getStatusCode());
        assertEquals("error.account.not_found", exception.getError().key());
        verify(groupAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("attachAccount should throw a 400 when the account is already attached")
    void testAttachAccount_shouldThrowWhenAlreadyAttached() {
        var groupId = UUID.randomUUID();
        var accountId = UUID.randomUUID();
        when(groupRepository.existsById(groupId)).thenReturn(true);
        when(accountRepository.existsById(accountId)).thenReturn(true);
        when(groupAccountRepository.existsByGroupIdAndAccountId(groupId, accountId)).thenReturn(true);

        var exception = assertThrows(ApiException.class, () -> service.attachAccount(
            userPrincipal, groupId, new GroupAccountRecord(accountId, Map.of())));

        assertEquals(400, exception.getStatusCode());
        assertEquals("error.group.account.already_attached", exception.getError().key());
        verify(groupAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("detachAccount should delete the relationship")
    void testDetachAccount_shouldDeleteRelationship() {
        var groupId = UUID.randomUUID();
        var accountId = UUID.randomUUID();
        var relation = GroupAccount.builder().id(UUID.randomUUID()).groupId(groupId).accountId(accountId).build();
        when(groupRepository.existsById(groupId)).thenReturn(true);
        when(groupAccountRepository.findByGroupIdAndAccountId(groupId, accountId)).thenReturn(Optional.of(relation));

        service.detachAccount(userPrincipal, groupId, accountId);

        verify(groupAccountRepository).delete(relation);
    }

    @Test
    @DisplayName("detachAccount should throw a 404 when the account is not attached")
    void testDetachAccount_shouldThrowWhenNotAttached() {
        var groupId = UUID.randomUUID();
        var accountId = UUID.randomUUID();
        when(groupRepository.existsById(groupId)).thenReturn(true);
        when(groupAccountRepository.findByGroupIdAndAccountId(groupId, accountId)).thenReturn(Optional.empty());

        var exception = assertThrows(ApiException.class,
            () -> service.detachAccount(userPrincipal, groupId, accountId));

        assertEquals(404, exception.getStatusCode());
        assertEquals("error.group.account.not_attached", exception.getError().key());
        verify(groupAccountRepository, never()).delete(any(GroupAccount.class));
    }

    @Test
    @DisplayName("update should replace the stored extra parameters when the record provides them")
    void testUpdate_shouldOverrideExtraParametersWhenProvided() {
        var id = UUID.randomUUID();
        var withExtraParameters = new GroupRecord("developers", "Developers", null, null, null, null, null,
            Map.of("team", "platform"));
        var existing = Group.builder().id(id).code("developers").extraParameters(Map.of("team", "core")).build();
        when(groupRepository.findById(id)).thenReturn(Optional.of(existing));
        when(groupRepository.existsByCodeAndIdNot("developers", id)).thenReturn(false);
        when(groupRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.update(userPrincipal, id, withExtraParameters);

        assertEquals(Map.of("team", "platform"), result.getExtraParameters());
    }
}
