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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.linagora.linid.im.api.model.application.role.ApplicationRoleMapper;
import io.github.linagora.linid.im.api.model.application.role.ApplicationRoleRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.ApplicationRole;
import io.github.linagora.linid.im.api.persistence.model.ApplicationRoleView;
import io.github.linagora.linid.im.api.persistence.model.ApplicationRoleViewQueryFilterDto;
import io.github.linagora.linid.im.api.persistence.repository.ApplicationRepository;
import io.github.linagora.linid.im.api.persistence.repository.ApplicationRoleRepository;
import io.github.linagora.linid.im.api.persistence.repository.ApplicationRoleViewRepository;
import io.github.linagora.linid.im.api.service.validation.SystemApplicationValidator;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: ApplicationRoleServiceImpl")
class ApplicationRoleServiceImplTest {

    @Mock
    private ApplicationRoleRepository applicationRoleRepository;

    @Mock
    private ApplicationRoleViewRepository applicationRoleViewRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ApplicationRoleMapper mapper;

    @Mock
    private SystemApplicationValidator systemApplicationValidator;

    @InjectMocks
    private ApplicationRoleServiceImpl service;

    private UserPrincipal userPrincipal;

    private UUID applicationId;

    private ApplicationRoleRecord record;

    @BeforeEach
    void setUp() {
        userPrincipal = new UserPrincipal();
        userPrincipal.setId(UUID.randomUUID());
        userPrincipal.setEmail("admin@example.com");
        applicationId = UUID.randomUUID();
        record = new ApplicationRoleRecord("admin", "Grants full administrative access");
    }

    @Test
    @DisplayName("create should persist the mapped role scoped to the application")
    void testCreate() {
        var mapped = ApplicationRole.builder().name("admin").build();
        when(applicationRepository.existsById(applicationId)).thenReturn(true);
        when(applicationRoleRepository.existsByApplicationIdAndName(applicationId, "admin")).thenReturn(false);
        when(mapper.toEntity(record, userPrincipal)).thenReturn(mapped);
        when(applicationRoleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(userPrincipal, applicationId, record);

        var captor = ArgumentCaptor.forClass(ApplicationRole.class);
        verify(applicationRoleRepository).save(captor.capture());
        var saved = captor.getValue();

        assertSame(mapped, saved);
        assertSame(mapped, result);
        assertEquals(applicationId, saved.getApplicationId());
    }

    @Test
    @DisplayName("create should throw when the application does not exist")
    void testCreate_shouldThrowWhenApplicationAbsent() {
        when(applicationRepository.existsById(applicationId)).thenReturn(false);

        var exception = assertThrows(ApiException.class,
            () -> service.create(userPrincipal, applicationId, record));

        assertEquals(404, exception.getStatusCode());
        assertEquals("error.application.not_found", exception.getError().key());
        verify(applicationRoleRepository, never()).save(any());
    }

    @Test
    @DisplayName("create should throw when the role name is already used in the application")
    void testCreate_shouldThrowWhenNameAlreadyExists() {
        when(applicationRepository.existsById(applicationId)).thenReturn(true);
        when(applicationRoleRepository.existsByApplicationIdAndName(applicationId, "admin")).thenReturn(true);

        var exception = assertThrows(ApiException.class,
            () -> service.create(userPrincipal, applicationId, record));

        assertEquals(400, exception.getStatusCode());
        assertEquals("error.application_role.name.already_exists", exception.getError().key());
        verify(applicationRoleRepository, never()).save(any());
    }

    @Test
    @DisplayName("findAll should return the roles of the application")
    void testFindAll() {
        when(applicationRepository.existsById(applicationId)).thenReturn(true);
        when(applicationRoleViewRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(new ApplicationRoleView())));

        var result = service.findAll(userPrincipal, applicationId,
            new ApplicationRoleViewQueryFilterDto(), Pageable.unpaged());

        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("findAll should throw when the application does not exist")
    void testFindAll_shouldThrowWhenApplicationAbsent() {
        when(applicationRepository.existsById(applicationId)).thenReturn(false);
        var filters = new ApplicationRoleViewQueryFilterDto();
        var pageable = Pageable.unpaged();

        var exception = assertThrows(ApiException.class,
            () -> service.findAll(userPrincipal, applicationId, filters, pageable));

        assertEquals(404, exception.getStatusCode());
        assertEquals("error.application.not_found", exception.getError().key());
    }

    @Test
    @DisplayName("findById should return the role scoped to the application")
    void testFindById() {
        var id = UUID.randomUUID();
        var entity = ApplicationRole.builder().id(id).build();
        when(applicationRepository.existsById(applicationId)).thenReturn(true);
        when(applicationRoleRepository.findByIdAndApplicationId(id, applicationId)).thenReturn(Optional.of(entity));

        assertSame(entity, service.findById(userPrincipal, applicationId, id));
    }

    @Test
    @DisplayName("findById should throw when the role does not exist")
    void testFindById_shouldThrowWhenAbsent() {
        var id = UUID.randomUUID();
        when(applicationRepository.existsById(applicationId)).thenReturn(true);
        when(applicationRoleRepository.findByIdAndApplicationId(id, applicationId)).thenReturn(Optional.empty());

        var exception = assertThrows(ApiException.class,
            () -> service.findById(userPrincipal, applicationId, id));

        assertEquals(404, exception.getStatusCode());
        assertEquals("error.application_role.not_found", exception.getError().key());
    }

    @Test
    @DisplayName("findById should throw when the application does not exist")
    void testFindById_shouldThrowWhenApplicationAbsent() {
        var id = UUID.randomUUID();
        when(applicationRepository.existsById(applicationId)).thenReturn(false);

        var exception = assertThrows(ApiException.class,
            () -> service.findById(userPrincipal, applicationId, id));

        assertEquals(404, exception.getStatusCode());
        assertEquals("error.application.not_found", exception.getError().key());
    }

    @Test
    @DisplayName("findViewById should return the role view scoped to the application")
    void testFindViewById() {
        var id = UUID.randomUUID();
        var entity = ApplicationRoleView.builder().id(id).build();
        when(applicationRepository.existsById(applicationId)).thenReturn(true);
        when(applicationRoleViewRepository.findByIdAndApplicationId(id, applicationId))
            .thenReturn(Optional.of(entity));

        assertSame(entity, service.findViewById(userPrincipal, applicationId, id));
    }

    @Test
    @DisplayName("findViewById should throw when the role does not exist")
    void testFindViewById_shouldThrowWhenAbsent() {
        var id = UUID.randomUUID();
        when(applicationRepository.existsById(applicationId)).thenReturn(true);
        when(applicationRoleViewRepository.findByIdAndApplicationId(id, applicationId))
            .thenReturn(Optional.empty());

        var exception = assertThrows(ApiException.class,
            () -> service.findViewById(userPrincipal, applicationId, id));

        assertEquals(404, exception.getStatusCode());
        assertEquals("error.application_role.not_found", exception.getError().key());
    }

    @Test
    @DisplayName("findViewById should throw when the application does not exist")
    void testFindViewById_shouldThrowWhenApplicationAbsent() {
        var id = UUID.randomUUID();
        when(applicationRepository.existsById(applicationId)).thenReturn(false);

        var exception = assertThrows(ApiException.class,
            () -> service.findViewById(userPrincipal, applicationId, id));

        assertEquals(404, exception.getStatusCode());
        assertEquals("error.application.not_found", exception.getError().key());
    }

    @Test
    @DisplayName("update should apply the record fields and persist")
    void testUpdate() {
        var id = UUID.randomUUID();
        var entity = ApplicationRole.builder().id(id).name("user").description("Old").build();
        when(applicationRepository.existsById(applicationId)).thenReturn(true);
        when(applicationRoleRepository.findByIdAndApplicationId(id, applicationId)).thenReturn(Optional.of(entity));
        when(applicationRoleRepository.existsByApplicationIdAndNameAndIdNot(applicationId, "admin", id))
            .thenReturn(false);
        when(applicationRoleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.update(userPrincipal, applicationId, id, record);

        assertEquals("admin", result.getName());
        assertEquals("Grants full administrative access", result.getDescription());
        assertEquals(userPrincipal.getId(), result.getUpdatedBy());
    }

    @Test
    @DisplayName("update should not check the name uniqueness when it is unchanged")
    void testUpdate_shouldSkipUniquenessCheckWhenNameUnchanged() {
        var id = UUID.randomUUID();
        var entity = ApplicationRole.builder().id(id).name("admin").build();
        when(applicationRepository.existsById(applicationId)).thenReturn(true);
        when(applicationRoleRepository.findByIdAndApplicationId(id, applicationId)).thenReturn(Optional.of(entity));
        when(applicationRoleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(userPrincipal, applicationId, id, record);

        verify(applicationRoleRepository, never()).existsByApplicationIdAndNameAndIdNot(any(), any(), any());
    }

    @Test
    @DisplayName("update should throw when the new role name is already used in the application")
    void testUpdate_shouldThrowWhenNameAlreadyExists() {
        var id = UUID.randomUUID();
        var entity = ApplicationRole.builder().id(id).name("user").build();
        when(applicationRepository.existsById(applicationId)).thenReturn(true);
        when(applicationRoleRepository.findByIdAndApplicationId(id, applicationId)).thenReturn(Optional.of(entity));
        when(applicationRoleRepository.existsByApplicationIdAndNameAndIdNot(applicationId, "admin", id))
            .thenReturn(true);

        var exception = assertThrows(ApiException.class,
            () -> service.update(userPrincipal, applicationId, id, record));

        assertEquals(400, exception.getStatusCode());
        assertEquals("error.application_role.name.already_exists", exception.getError().key());
        verify(applicationRoleRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteById should delete the role scoped to the application")
    void testDeleteById() {
        var id = UUID.randomUUID();
        var entity = ApplicationRole.builder().id(id).build();
        when(applicationRepository.existsById(applicationId)).thenReturn(true);
        when(applicationRoleRepository.findByIdAndApplicationId(id, applicationId)).thenReturn(Optional.of(entity));

        service.deleteById(userPrincipal, applicationId, id);

        var captor = ArgumentCaptor.forClass(ApplicationRole.class);
        verify(applicationRoleRepository).delete(captor.capture());
        assertSame(entity, captor.getValue());
    }

    @Test
    @DisplayName("deleteById should throw when the role does not exist")
    void testDeleteById_shouldThrowWhenAbsent() {
        var id = UUID.randomUUID();
        when(applicationRepository.existsById(applicationId)).thenReturn(true);
        when(applicationRoleRepository.findByIdAndApplicationId(id, applicationId)).thenReturn(Optional.empty());

        var exception = assertThrows(ApiException.class,
            () -> service.deleteById(userPrincipal, applicationId, id));

        assertEquals(404, exception.getStatusCode());
        assertEquals("error.application_role.not_found", exception.getError().key());
        verify(applicationRoleRepository, never()).delete(any(ApplicationRole.class));
    }

    @Test
    @DisplayName("create should not persist anything when the application is system-reserved")
    void testCreate_shouldNotPersistWhenSystemReserved() {
        when(applicationRepository.existsById(applicationId)).thenReturn(true);
        doThrow(new ApiException(400, I18nMessage.of("error.application_role.system_reserved")))
            .when(systemApplicationValidator).ensureRolesAreMutable(applicationId);

        var exception = assertThrows(ApiException.class,
            () -> service.create(userPrincipal, applicationId, record));

        assertEquals(400, exception.getStatusCode());
        assertEquals("error.application_role.system_reserved", exception.getError().key());
        verify(applicationRoleRepository, never()).save(any());
    }

    @Test
    @DisplayName("update should not persist anything when the application is system-reserved")
    void testUpdate_shouldNotPersistWhenSystemReserved() {
        var id = UUID.randomUUID();
        var entity = ApplicationRole.builder().id(id).name("Administrator").build();
        when(applicationRepository.existsById(applicationId)).thenReturn(true);
        when(applicationRoleRepository.findByIdAndApplicationId(id, applicationId)).thenReturn(Optional.of(entity));
        doThrow(new ApiException(400, I18nMessage.of("error.application_role.system_reserved")))
            .when(systemApplicationValidator).ensureRolesAreMutable(applicationId);

        var exception = assertThrows(ApiException.class,
            () -> service.update(userPrincipal, applicationId, id, record));

        assertEquals(400, exception.getStatusCode());
        assertEquals("error.application_role.system_reserved", exception.getError().key());
        verify(applicationRoleRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteById should not delete anything when the application is system-reserved")
    void testDeleteById_shouldNotDeleteWhenSystemReserved() {
        var id = UUID.randomUUID();
        var entity = ApplicationRole.builder().id(id).build();
        when(applicationRepository.existsById(applicationId)).thenReturn(true);
        when(applicationRoleRepository.findByIdAndApplicationId(id, applicationId)).thenReturn(Optional.of(entity));
        doThrow(new ApiException(400, I18nMessage.of("error.application_role.system_reserved")))
            .when(systemApplicationValidator).ensureRolesAreMutable(applicationId);

        var exception = assertThrows(ApiException.class,
            () -> service.deleteById(userPrincipal, applicationId, id));

        assertEquals(400, exception.getStatusCode());
        assertEquals("error.application_role.system_reserved", exception.getError().key());
        verify(applicationRoleRepository, never()).delete(any(ApplicationRole.class));
    }
}
