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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.linagora.linid.im.api.model.role.RoleMapper;
import io.github.linagora.linid.im.api.model.role.RoleRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.Role;
import io.github.linagora.linid.im.api.persistence.model.RoleView;
import io.github.linagora.linid.im.api.persistence.model.RoleViewQueryFilterDto;
import io.github.linagora.linid.im.api.persistence.repository.RoleRepository;
import io.github.linagora.linid.im.api.persistence.repository.RoleViewRepository;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
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
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: RoleServiceImpl")
class RoleServiceImplTest {

    private static final UUID ADMIN_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RoleViewRepository roleViewRepository;

    @Mock
    private RoleMapper roleMapper;

    @InjectMocks
    private RoleServiceImpl roleService;

    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        userPrincipal = new UserPrincipal();
        userPrincipal.setId(ADMIN_ID);
        userPrincipal.setEmail("admin@example.com");
    }

    private RoleRecord createSampleRecord() {
        return new RoleRecord(
                "ADMINISTRATOR",
                "Administrator",
                "System administrator",
                Map.of("scope", "global")
        );
    }

    private Role createSampleRole(UUID uuid) {
        return Role.builder()
                .id(UUID.randomUUID())
                .code("ADMINISTRATOR")
                .name("Administrator")
                .description("System administrator")
                .extraParameters(Map.of("scope", "global"))
                .createdBy(uuid)
                .updatedBy(uuid)
                .insertDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
    }

    private RoleView createSampleRoleView(final UUID id) {
        return RoleView.builder()
                .id(id)
                .code("ADMINISTRATOR")
                .name("Administrator")
                .description("System administrator")
                .extraParameters(Map.of("scope", "global"))
                .createdBy("Admin User")
                .updatedBy("Admin User")
                .build();
    }

    @Test
    @DisplayName("Should create role with mapped entity and save it")
    void testCreate_shouldMapAndSaveRole() {
        var uuid = UUID.randomUUID();
        var record = createSampleRecord();
        var entity = createSampleRole(uuid);

        when(roleRepository.existsByCode(record.code())).thenReturn(false);
        when(roleMapper.toRole(record, userPrincipal)).thenReturn(entity);
        when(roleRepository.save(entity)).thenReturn(entity);

        Role result = roleService.create(userPrincipal, record);

        assertNotNull(result);
        assertSame(entity, result);
        assertEquals("ADMINISTRATOR", result.getCode());
        assertEquals("Administrator", result.getName());
        assertEquals("System administrator", result.getDescription());
        assertEquals(Map.of("scope", "global"), result.getExtraParameters());
        assertEquals(uuid, result.getCreatedBy());
        assertEquals(uuid, result.getUpdatedBy());

        verify(roleRepository).existsByCode(record.code());
        verify(roleMapper).toRole(record, userPrincipal);
        verify(roleRepository).save(entity);
    }

    @Test
    @DisplayName("Should throw 400 when creating a role with an existing code")
    void testCreate_shouldThrow400WhenCodeAlreadyExists() {
        var record = createSampleRecord();

        when(roleRepository.existsByCode(record.code())).thenReturn(true);

        ApiException exception = assertThrows(
            ApiException.class,
            () -> roleService.create(userPrincipal, record)
        );

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getStatusCode());
        assertEquals(
            "error.role.code.already_exists",
            exception.getError().key()
        );

        verify(roleRepository).existsByCode(record.code());
        verify(roleMapper, never()).toRole(record, userPrincipal);
        verify(roleRepository, never()).save(any(Role.class));
    }

    @Test
    @DisplayName("Should delegate role listing to the repository with specification and pageable")
    void testFindAll_shouldDelegateToRepository() {
        var pageable = PageRequest.of(0, 10);
        var filters = new RoleViewQueryFilterDto();
        var entity = createSampleRoleView(UUID.randomUUID());

        when(roleViewRepository.findAll(
                any(Specification.class),
                eq(pageable)
        )).thenReturn(new PageImpl<>(List.of(entity)));

        Page<RoleView> result =
                roleService.findAll(userPrincipal, filters, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(entity.getId(), result.getContent().getFirst().getId());
        assertEquals(entity.getCode(), result.getContent().getFirst().getCode());

        verify(roleViewRepository).findAll(
                any(Specification.class),
                eq(pageable)
        );
    }

    @Test
    @DisplayName("Should return role view when found by ID")
    void testFindById_shouldReturnRoleWhenFound() {
        UUID id = UUID.randomUUID();
        var entity = createSampleRoleView(id);

        when(roleViewRepository.findById(id)).thenReturn(Optional.of(entity));

        RoleView result = roleService.findById(userPrincipal, id);

        assertNotNull(result);
        assertSame(entity, result);
        assertEquals(id, result.getId());

        verify(roleViewRepository).findById(id);
    }

    @Test
    @DisplayName("Should throw ApiException 404 when role is not found")
    void testFindById_shouldThrow404WhenNotFound() {
        UUID id = UUID.randomUUID();

        when(roleViewRepository.findById(id)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> roleService.findById(userPrincipal, id)
        );

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.getStatusCode());
        assertEquals(
                "error.role.not_found",
                exception.getError().key()
        );

        verify(roleViewRepository).findById(id);
    }

    @Test
    @DisplayName("Should update role fields, save and return the refreshed view")
    void testUpdate_shouldApplyFieldsSaveAndReturnView() {
        UUID id = UUID.randomUUID();

        var existing = Role.builder()
            .id(id)
            .code("OLD_CODE")
            .name("Old name")
            .description("Old description")
            .extraParameters(Map.of("scope", "old"))
            .createdBy(ADMIN_ID)
            .updatedBy(ADMIN_ID)
            .build();

        var record = new RoleRecord(
            "UPDATED_ROLE",
            "Updated role",
            "Updated description",
            Map.of("scope", "updated")
        );

        var view = createSampleRoleView(id);
        view.setCode(record.code());
        view.setName(record.name());
        view.setDescription(record.description());
        view.setExtraParameters(record.extraParameters());

        when(roleRepository.findById(id)).thenReturn(Optional.of(existing));
        when(roleRepository.existsByCodeAndIdNot(record.code(), id)).thenReturn(false);
        when(roleRepository.saveAndFlush(existing)).thenReturn(existing);
        when(roleViewRepository.findById(id)).thenReturn(Optional.of(view));

        RoleView result = roleService.update(userPrincipal, id, record);

        assertSame(view, result);

        assertEquals(record.code(), existing.getCode());
        assertEquals(record.name(), existing.getName());
        assertEquals(record.description(), existing.getDescription());
        assertEquals(record.extraParameters(), existing.getExtraParameters());
        assertEquals(ADMIN_ID, existing.getUpdatedBy());

        verify(roleRepository).findById(id);
        verify(roleRepository).existsByCodeAndIdNot(record.code(), id);
        verify(roleRepository).saveAndFlush(existing);
        verify(roleViewRepository).findById(id);
    }

    @Test
    @DisplayName("Should throw 404 when updating a non-existent role")
    void testUpdate_shouldThrow404WhenRoleNotFound() {
        UUID id = UUID.randomUUID();
        var record = createSampleRecord();

        when(roleRepository.findById(id)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> roleService.update(userPrincipal, id, record)
        );

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.getStatusCode());
        assertEquals(
                "error.role.not_found",
                exception.getError().key()
        );

        verify(roleRepository).findById(id);
        verify(roleRepository, never()).saveAndFlush(any(Role.class));
        verify(roleViewRepository, never()).findById(any(UUID.class));
    }

    @Test
    @DisplayName("Should delete role when it exists")
    void testDeleteById_shouldDeleteWhenFound() {
        UUID id = UUID.randomUUID();
        var entity = createSampleRoleView(id);

        when(roleViewRepository.findById(id)).thenReturn(Optional.of(entity));

        roleService.deleteById(userPrincipal, id);

        verify(roleViewRepository).findById(id);
        verify(roleRepository).deleteById(id);
    }

    @Test
    @DisplayName("Should throw ApiException 404 when deleting a non-existent role")
    void testDeleteById_shouldThrow404WhenNotFound() {
        UUID id = UUID.randomUUID();

        when(roleViewRepository.findById(id)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> roleService.deleteById(userPrincipal, id)
        );

        assertEquals(HttpStatus.NOT_FOUND.value(), exception.getStatusCode());
        assertEquals(
                "error.role.not_found",
                exception.getError().key()
        );

        verify(roleViewRepository).findById(id);
        verify(roleRepository, never()).deleteById(any(UUID.class));
    }

    @Test
    @DisplayName("Should throw 400 when updating a role with an existing code")
    void testUpdate_shouldThrow400WhenCodeAlreadyExists() {
        UUID id = UUID.randomUUID();

        var existing = Role.builder()
            .id(id)
            .code("OLD_CODE")
            .name("Old name")
            .description("Old description")
            .extraParameters(Map.of())
            .createdBy(ADMIN_ID)
            .updatedBy(ADMIN_ID)
            .build();

        var record = new RoleRecord(
            "ADMINISTRATOR",
            "Administrator",
            "Description",
            Map.of()
        );

        when(roleRepository.findById(id)).thenReturn(Optional.of(existing));
        when(roleRepository.existsByCodeAndIdNot(record.code(), id))
            .thenReturn(true);

        ApiException exception = assertThrows(
            ApiException.class,
            () -> roleService.update(userPrincipal, id, record)
        );

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getStatusCode());
        assertEquals(
            "error.role.code.already_exists",
            exception.getError().key()
        );

        verify(roleRepository).findById(id);
        verify(roleRepository).existsByCodeAndIdNot(record.code(), id);
        verify(roleRepository, never()).saveAndFlush(any(Role.class));
        verify(roleViewRepository, never()).findById(any(UUID.class));
    }
}