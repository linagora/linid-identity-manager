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

import io.github.linagora.linid.im.api.model.common.PeriodRecord;
import io.github.linagora.linid.im.api.model.organizationalunit.OrganizationalUnitMapper;
import io.github.linagora.linid.im.api.model.organizationalunit.OrganizationalUnitRecord;
import io.github.linagora.linid.im.api.model.organizationalunit.OrganizationalUnitRelationMapper;
import io.github.linagora.linid.im.api.model.organizationalunit.OrganizationalUnitStatusMapper;
import io.github.linagora.linid.im.api.model.organizationalunit.OrganizationalUnitStatusRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.OrganizationalUnit;
import io.github.linagora.linid.im.api.persistence.model.OrganizationalUnitQueryFilterDto;
import io.github.linagora.linid.im.api.persistence.model.OrganizationalUnitRelation;
import io.github.linagora.linid.im.api.persistence.model.OrganizationalUnitStatus;
import io.github.linagora.linid.im.api.persistence.model.OrganizationalUnitView;
import io.github.linagora.linid.im.api.persistence.model.OrganizationalUnitViewQueryFilterDto;
import io.github.linagora.linid.im.api.persistence.repository.OrganizationalUnitRelationRepository;
import io.github.linagora.linid.im.api.persistence.repository.OrganizationalUnitRepository;
import io.github.linagora.linid.im.api.persistence.repository.OrganizationalUnitStatusRepository;
import io.github.linagora.linid.im.api.persistence.repository.OrganizationalUnitViewRepository;
import io.github.linagora.linid.im.api.service.validation.OrganizationalUnitStatusValidator;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.zorin95670.specification.SpringQueryFilterSpecification;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: OrganizationalUnitServiceImpl")
class OrganizationalUnitServiceImplTest {
    @Mock
    private OrganizationalUnitRepository organizationalUnitRepository;

    @Mock
    private OrganizationalUnitViewRepository organizationalUnitViewRepository;

    @Mock
    private OrganizationalUnitRelationRepository organizationalUnitRelationRepository;

    @Mock
    private OrganizationalUnitMapper mapper;

    @Mock
    private OrganizationalUnitRelationMapper relationMapper;

    @Mock
    private OrganizationalUnitStatusRepository organizationalUnitStatusRepository;

    @Mock
    private OrganizationalUnitStatusMapper organizationalUnitStatusMapper;

    @Mock
    private OrganizationalUnitStatusValidator organizationalUnitStatusValidator;

    @InjectMocks
    private OrganizationalUnitServiceImpl service;

    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        userPrincipal = new UserPrincipal();
        userPrincipal.setId(UUID.randomUUID());
        userPrincipal.setEmail("admin@example.com");
    }

    @Test
    @DisplayName("should retrieve once the root organizational unit")
    void testGetRoot_shouldRetrieveOnce() {
        var root = OrganizationalUnit.builder()
            .id(UUID.randomUUID())
            .name("root")
            .type("root")
            .build();

        when(organizationalUnitRepository.findByNameAndType(any(), any())).thenReturn(Optional.of(root));

        var value = service.getRoot();
        assertEquals(root, value);

        value = service.getRoot();
        assertEquals(root, value);
        verify(organizationalUnitRepository, times(1)).findByNameAndType(any(), any());
    }

    @Test
    @DisplayName("should throw exception without root organizational unit")
    void testGetRoot_shouldThrowException() {
        when(organizationalUnitRepository.findByNameAndType(any(), any())).thenReturn(Optional.empty());

        var exception = assertThrows(ApiException.class,
            () -> service.getRoot());
        assertEquals(500, exception.getStatusCode());
        assertEquals("error.organizational.unit.root.not_found", exception.getError().key());
    }

    @Test
    @DisplayName("should throw exception with root name")
    void testCreate_shouldThrowExceptionOnRootName() {
        var entity = new OrganizationalUnitRecord(UUID.randomUUID(), "root", "test");

        var exception = assertThrows(ApiException.class,
            () -> service.create(userPrincipal, entity));
        assertEquals(400, exception.getStatusCode());
        assertEquals("error.organizational.unit.root", exception.getError().key());
    }

    @Test
    @DisplayName("should throw exception with root type")
    void testCreate_shouldThrowExceptionOnRootType() {
        var entity = new OrganizationalUnitRecord(UUID.randomUUID(), "test", "root");

        var exception = assertThrows(ApiException.class,
            () -> service.create(userPrincipal, entity));
        assertEquals(400, exception.getStatusCode());
        assertEquals("error.organizational.unit.root", exception.getError().key());
    }

    @Test
    @DisplayName("should throw exception with unknown parent")
    void testCreate_shouldThrowExceptionOnUnknownParent() {
        var uuid = UUID.randomUUID();
        var entity = new OrganizationalUnitRecord(uuid, "test", "test");
        when(organizationalUnitRepository.findById(uuid)).thenReturn(Optional.empty());

        var exception = assertThrows(ApiException.class,
            () -> service.create(userPrincipal, entity));
        assertEquals(404, exception.getStatusCode());
        assertEquals("error.organizational.unit.not_found", exception.getError().key());
        assertEquals(uuid.toString(), exception.getError().context().get("id"));
        verify(organizationalUnitRepository, never()).save(any());
        verify(organizationalUnitRelationRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw exception with same name and type")
    void testCreate_shouldThrowExceptionOnSameNameAndType() {
        var uuid = UUID.randomUUID();
        var entity = new OrganizationalUnitRecord(uuid, "test", "test");
        when(organizationalUnitRepository.findById(uuid)).thenReturn(Optional.of(new OrganizationalUnit()));
        when(organizationalUnitRepository.findByNameAndType("test", "test")).thenReturn(Optional.of(new OrganizationalUnit()));

        var exception = assertThrows(ApiException.class,
            () -> service.create(userPrincipal, entity));
        assertEquals(400, exception.getStatusCode());
        assertEquals("error.organizational.unit.already_exists", exception.getError().key());
        assertEquals("test", exception.getError().context().get("name"));
        assertEquals("test", exception.getError().context().get("type"));
        verify(organizationalUnitRepository, never()).save(any());
        verify(organizationalUnitRelationRepository, never()).save(any());
    }

    @Test
    @DisplayName("should create valid organizational unit")
    void testCreate_shouldCreateValidOrganizationalUnit() {
        var uuid = UUID.randomUUID();
        var root = OrganizationalUnit.builder()
            .id(uuid)
            .name("root")
            .type("root")
            .build();

        var expected = OrganizationalUnit.builder()
            .id(UUID.randomUUID())
            .name("test")
            .type("test")
            .build();
        var entity = new OrganizationalUnitRecord(uuid, "test", "test");
        var relation = OrganizationalUnitRelation.builder()
            .id(UUID.randomUUID())
            .parentId(root.getId())
            .childId(expected.getId())
            .build();

        when(organizationalUnitRepository.findById(uuid)).thenReturn(Optional.of(root));
        when(organizationalUnitRepository.findByNameAndType(any(), any())).thenReturn(Optional.empty());
        when(organizationalUnitRepository.save(any())).thenReturn(expected);
        when(relationMapper.toEntity(any(), any(), any())).thenReturn(relation);
        when(organizationalUnitRelationRepository.save(relation)).thenReturn(relation);

        var result = service.create(userPrincipal, entity);
        assertEquals(expected, result);
    }

    @Test
    @DisplayName("should throw exception with unknown entity")
    void testFindById_shouldThrowExceptionOnUnknownEntity() {
        var uuid = UUID.randomUUID();
        when(organizationalUnitRepository.findById(uuid)).thenReturn(Optional.empty());

        var exception = assertThrows(ApiException.class,
            () -> service.findById(userPrincipal, uuid));
        assertEquals(404, exception.getStatusCode());
        assertEquals("error.organizational.unit.not_found", exception.getError().key());
        assertEquals(uuid.toString(), exception.getError().context().get("id"));
    }

    @Test
    @DisplayName("Should call repository with specification and pageable")
    void testFindAll_shouldDelegateToRepository() {
        var pageable = PageRequest.of(0, 10);
        var entity = new OrganizationalUnitView();
        var filters = new OrganizationalUnitViewQueryFilterDto();
        when(organizationalUnitViewRepository.findAll(
            ArgumentMatchers.<Specification<OrganizationalUnitView>>any(),
            any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(entity)));

        Page<OrganizationalUnitView> result = service.findAll(userPrincipal, filters, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(organizationalUnitViewRepository).findAll(
            ArgumentMatchers.<Specification<OrganizationalUnitView>>any(),
            any(Pageable.class));
    }

    @Test
    @DisplayName("should throw exception with trying to delete root")
    void testDeleteById_shouldThrowExceptionOnDeleteRoot() {
        var uuid = UUID.randomUUID();
        var root = OrganizationalUnit.builder()
            .id(uuid)
            .name("root")
            .type("root")
            .build();

        when(organizationalUnitRepository.findByNameAndType(any(), any())).thenReturn(Optional.of(root));

        var exception = assertThrows(ApiException.class,
            () -> service.deleteById(userPrincipal, uuid));
        assertEquals(400, exception.getStatusCode());
        assertEquals("error.organizational.unit.root.delete", exception.getError().key());
    }

    @Test
    @DisplayName("should throw exception on unknown id")
    void testDeleteById_shouldThrowExceptionOnUnknownId() {
        var rootUuid = UUID.randomUUID();
        var uuid = UUID.randomUUID();
        var root = OrganizationalUnit.builder()
            .id(rootUuid)
            .name("root")
            .type("root")
            .build();

        when(organizationalUnitRepository.findByNameAndType(any(), any())).thenReturn(Optional.of(root));
        when(organizationalUnitRepository.findById(any())).thenReturn(Optional.empty());

        var exception = assertThrows(ApiException.class,
            () -> service.deleteById(userPrincipal, uuid));
        assertEquals(404, exception.getStatusCode());
        assertEquals("error.organizational.unit.not_found", exception.getError().key());
        assertEquals(uuid.toString(), exception.getError().context().get("id"));
    }

    @Test
    @DisplayName("should throw exception when updating root organizational unit")
    void testUpdate_shouldThrowExceptionOnRootUpdate() {
        var rootId = UUID.randomUUID();

        var root = OrganizationalUnit.builder()
            .id(rootId)
            .name("root")
            .type("root")
            .build();

        when(organizationalUnitRepository.findByNameAndType(any(), any()))
            .thenReturn(Optional.of(root));

        var record = new OrganizationalUnitRecord(rootId, "test", "test");

        var exception = assertThrows(ApiException.class, () -> service.update(userPrincipal, rootId, record));

        assertEquals(400, exception.getStatusCode());
        assertEquals("error.organizational.unit.root.update", exception.getError().key());
    }

    @Test
    @DisplayName("should throw exception when updating with root name")
    void testUpdate_shouldThrowExceptionOnRootName() {
        var rootId = UUID.randomUUID();
        var uuid = UUID.randomUUID();

        var root = OrganizationalUnit.builder()
            .id(rootId)
            .name("root")
            .type("root")
            .build();

        when(organizationalUnitRepository.findByNameAndType(any(), any()))
            .thenReturn(Optional.of(root));

        var record = new OrganizationalUnitRecord(UUID.randomUUID(), "root", "test");

        var exception = assertThrows(ApiException.class,
            () -> service.update(userPrincipal, uuid, record));

        assertEquals(400, exception.getStatusCode());
        assertEquals("error.organizational.unit.root", exception.getError().key());
    }

    @Test
    @DisplayName("should throw exception when updating with root type")
    void testUpdate_shouldThrowExceptionOnRootType() {
        var rootId = UUID.randomUUID();
        var uuid = UUID.randomUUID();

        var root = OrganizationalUnit.builder()
            .id(rootId)
            .name("root")
            .type("root")
            .build();

        when(organizationalUnitRepository.findByNameAndType(any(), any()))
            .thenReturn(Optional.of(root));
        var record = new OrganizationalUnitRecord(UUID.randomUUID(), "test", "root");

        var exception = assertThrows(ApiException.class,
            () -> service.update(userPrincipal, uuid, record));

        assertEquals(400, exception.getStatusCode());
        assertEquals("error.organizational.unit.root", exception.getError().key());
    }

    @Test
    @DisplayName("should throw exception when updating with already existing name and ou")
    void testUpdate_shouldThrowExceptionOnSameNameAndType() {
        var rootId = UUID.randomUUID();
        var uuid = UUID.randomUUID();

        var root = OrganizationalUnit.builder()
            .id(rootId)
            .name("root")
            .type("root")
            .build();

        when(organizationalUnitRepository.findByNameAndType(any(), any()))
            .thenReturn(Optional.of(root));
        when(organizationalUnitRepository.findAll(any(SpringQueryFilterSpecification.class))).thenReturn(List.of(new OrganizationalUnit()));

        var record = new OrganizationalUnitRecord(UUID.randomUUID(), "test", "test");

        var exception = assertThrows(ApiException.class,
            () -> service.update(userPrincipal, uuid, record));

        assertEquals(400, exception.getStatusCode());
        assertEquals("error.organizational.unit.already_exists", exception.getError().key());
        assertEquals("test", exception.getError().context().get("name"));
        assertEquals("test", exception.getError().context().get("type"));
    }

    @Test
    @DisplayName("should return existing entity when no changes are detected")
    void testUpdate_shouldReturnSameEntityWhenNoChange() {
        var rootId = UUID.randomUUID();
        var uuid = UUID.randomUUID();

        var root = OrganizationalUnit.builder()
            .id(rootId)
            .name("root")
            .type("root")
            .build();
        var entity = OrganizationalUnit.builder()
            .id(uuid)
            .name("same")
            .type("same")
            .build();

        when(organizationalUnitRepository.findById(uuid)).thenReturn(Optional.of(entity));
        when(organizationalUnitRepository.findByNameAndType(any(), any())).thenReturn(Optional.of(root));

        var record = new OrganizationalUnitRecord(UUID.randomUUID(), "same", "same");

        var result = service.update(userPrincipal, uuid, record);

        assertEquals(entity, result);

        verify(organizationalUnitRepository, times(0)).save(any());
    }

    @Test
    @DisplayName("should update and save organizational unit")
    void testUpdate_shouldUpdateAndSaveEntity() {
        var rootId = UUID.randomUUID();
        var uuid = UUID.randomUUID();

        var root = OrganizationalUnit.builder()
            .id(rootId)
            .name("root")
            .type("root")
            .build();
        var entity = OrganizationalUnit.builder()
            .id(uuid)
            .name("old")
            .type("old")
            .build();
        var updated = OrganizationalUnit.builder()
            .id(uuid)
            .name("new")
            .type("new")
            .build();

        when(organizationalUnitRepository.findById(uuid)).thenReturn(Optional.of(entity));
        when(organizationalUnitRepository.findByNameAndType(any(), any())).thenReturn(Optional.of(root));
        when(organizationalUnitRepository.save(any())).thenReturn(updated);

        var record = new OrganizationalUnitRecord(UUID.randomUUID(), "new", "new");

        var result = service.update(userPrincipal, uuid, record);

        assertEquals(updated, result);

        verify(organizationalUnitRepository, times(1)).save(entity);
    }

    @Test
    @DisplayName("should throw 404 when updating status of an unknown organizational unit")
    void testUpdateStatus_shouldThrowExceptionOnUnknownOrganizationalUnit() {
        var uuid = UUID.randomUUID();
        var record = new OrganizationalUnitStatusRecord(
            new PeriodRecord(OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(5)),
            "REORGANIZATION", "MERGER", "comment");

        when(organizationalUnitRepository.existsById(uuid)).thenReturn(false);

        var exception = assertThrows(ApiException.class,
            () -> service.updateStatus(userPrincipal, uuid, record));

        assertEquals(404, exception.getStatusCode());
        assertEquals("error.organizational.unit.not_found", exception.getError().key());
        assertEquals(uuid.toString(), exception.getError().context().get("id"));
        verify(organizationalUnitStatusRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("should throw 404 when the organizational unit has no status row")
    void testUpdateStatus_shouldThrowExceptionOnMissingStatusRow() {
        var uuid = UUID.randomUUID();
        var record = new OrganizationalUnitStatusRecord(
            new PeriodRecord(OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(5)),
            "REORGANIZATION", "MERGER", "comment");

        when(organizationalUnitRepository.existsById(uuid)).thenReturn(true);
        when(organizationalUnitStatusRepository.findByOrganizationalUnitId(uuid)).thenReturn(Optional.empty());

        var exception = assertThrows(ApiException.class,
            () -> service.updateStatus(userPrincipal, uuid, record));

        assertEquals(404, exception.getStatusCode());
        assertEquals("error.organizational.unit.status.not_found", exception.getError().key());
        assertEquals(uuid.toString(), exception.getError().context().get("id"));
        verify(organizationalUnitStatusRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("should update suspension status and return the refreshed view")
    void testUpdateStatus_shouldUpdateAndReturnView() {
        var uuid = UUID.randomUUID();
        var record = new OrganizationalUnitStatusRecord(
            new PeriodRecord(OffsetDateTime.now().plusDays(1), OffsetDateTime.now().plusDays(5)),
            "REORGANIZATION", "MERGER", "comment");
        var status = OrganizationalUnitStatus.builder()
            .id(UUID.randomUUID())
            .organizationalUnitId(uuid)
            .build();
        var updatedStatus = OrganizationalUnitStatus.builder()
            .id(status.getId())
            .organizationalUnitId(uuid)
            .build();
        var view = new OrganizationalUnitView();

        when(organizationalUnitRepository.existsById(uuid)).thenReturn(true);
        when(organizationalUnitStatusRepository.findByOrganizationalUnitId(uuid)).thenReturn(Optional.of(status));
        when(organizationalUnitStatusMapper.toOrganizationalUnitStatus(status, record, userPrincipal))
            .thenReturn(updatedStatus);
        when(organizationalUnitViewRepository.findById(uuid)).thenReturn(Optional.of(view));

        var result = service.updateStatus(userPrincipal, uuid, record);

        assertEquals(view, result);
        verify(organizationalUnitStatusValidator).validate(record, uuid);
        verify(organizationalUnitStatusRepository).saveAndFlush(updatedStatus);
    }

    @Test
    @DisplayName("should pass the request record straight to the mapper and validator")
    void testUpdateStatus_shouldDelegateRecordUnchanged() {
        var uuid = UUID.randomUUID();
        var record = new OrganizationalUnitStatusRecord(
            new PeriodRecord(OffsetDateTime.now().plusDays(2), null),
            "INVESTIGATION", null, null);
        var status = OrganizationalUnitStatus.builder()
            .id(UUID.randomUUID())
            .organizationalUnitId(uuid)
            .build();
        var view = new OrganizationalUnitView();

        when(organizationalUnitRepository.existsById(uuid)).thenReturn(true);
        when(organizationalUnitStatusRepository.findByOrganizationalUnitId(uuid)).thenReturn(Optional.of(status));
        when(organizationalUnitStatusMapper.toOrganizationalUnitStatus(status, record, userPrincipal))
            .thenReturn(status);
        when(organizationalUnitViewRepository.findById(uuid)).thenReturn(Optional.of(view));

        var result = service.updateStatus(userPrincipal, uuid, record);

        assertEquals(view, result);
        verify(organizationalUnitStatusValidator).validate(record, uuid);
        verify(organizationalUnitStatusMapper).toOrganizationalUnitStatus(status, record, userPrincipal);
    }
}
