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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.linagora.linid.im.api.model.role.RoleDTO;
import io.github.linagora.linid.im.api.model.role.RoleMapper;
import io.github.linagora.linid.im.api.model.role.RoleRecord;
import io.github.linagora.linid.im.api.model.role.RoleViewDTO;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.Role;
import io.github.linagora.linid.im.api.persistence.model.RoleView;
import io.github.linagora.linid.im.api.persistence.model.RoleViewQueryFilterDto;
import io.github.linagora.linid.im.api.service.RoleService;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
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
@DisplayName("Test class: RoleController")
class RoleControllerTest {

    @Mock
    private RoleService roleService;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private PagedResponseStatusResolver pagedResponseStatusResolver;

    @InjectMocks
    private RoleController roleController;

    private UserPrincipal userPrincipal;

    private static final UUID ADMIN_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static final String ADMIN_FULL_NAME = "Admin User";

    @BeforeEach
    void setUp() {
        userPrincipal = new UserPrincipal();
        userPrincipal.setId(ADMIN_ID);
        userPrincipal.setEmail("admin@example.com");
    }

    private RoleView createSampleViewEntity() {
        return RoleView.builder()
                .id(UUID.randomUUID())
                .code("ADMINISTRATOR")
                .name("Administrator")
                .description("System administrator")
                .extraParameters(Map.of("scope", "global"))
                .createdBy(ADMIN_FULL_NAME)
                .updatedBy(ADMIN_FULL_NAME)
                .insertDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
    }

    private RoleDTO createSampleDTO() {
        return RoleDTO.builder()
                .id(UUID.randomUUID())
                .code("ADMINISTRATOR")
                .name("Administrator")
                .description("System administrator")
                .extraParameters(Map.of("scope", "global"))
                .createdBy(ADMIN_ID)
                .updatedBy(ADMIN_ID)
                .insertDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
    }

    private RoleRecord createSampleRecord() {
        return new RoleRecord(
                "ADMINISTRATOR",
                "Administrator",
                "System administrator",
                Map.of("scope", "global")
        );
    }

    @Test
    @DisplayName("Should create role and return 201")
    void testCreate_shouldReturn201WithRoleDTO() {
        var request = createSampleRecord();
        var entity = Role.builder()
                .id(UUID.randomUUID())
                .code(request.code())
                .name(request.name())
                .description(request.description())
                .extraParameters(request.extraParameters())
                .createdBy(ADMIN_ID)
                .updatedBy(ADMIN_ID)
                .insertDate(OffsetDateTime.now())
                .updateDate(OffsetDateTime.now())
                .build();
        var dto = createSampleDTO();

        when(roleService.create(userPrincipal, request)).thenReturn(entity);
        when(roleMapper.toDTO(entity)).thenReturn(dto);

        ResponseEntity<RoleDTO> response =
                roleController.create(userPrincipal, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(dto.getId(), response.getBody().getId());
        assertEquals("ADMINISTRATOR", response.getBody().getCode());
        assertEquals("Administrator", response.getBody().getName());
        assertEquals("System administrator", response.getBody().getDescription());
        assertEquals(Map.of("scope", "global"), response.getBody().getExtraParameters());
        assertEquals(ADMIN_ID, response.getBody().getCreatedBy());
        assertEquals(ADMIN_ID, response.getBody().getUpdatedBy());

        verify(roleService).create(userPrincipal, request);
        verify(roleMapper).toDTO(entity);
    }

    @Test
    @DisplayName("Should return paginated roles with mapped DTOs")
    @SuppressWarnings("unchecked")
    void testFindAll_shouldReturnPaginatedRoleDTOs() {
        var entity = createSampleViewEntity();
        var filters = new RoleViewQueryFilterDto();
        var pageable = PageRequest.of(0, 10);
        var dto = RoleViewDTO.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .extraParameters(entity.getExtraParameters())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .insertDate(entity.getInsertDate())
                .updateDate(entity.getUpdateDate())
                .build();

        when(roleService.findAll(
                any(UserPrincipal.class),
                any(RoleViewQueryFilterDto.class),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        when(roleMapper.toDTO(entity)).thenReturn(dto);

        when(pagedResponseStatusResolver.resolve(any(Page.class)))
                .thenAnswer(invocation ->
                        ResponseEntity.ok(invocation.getArgument(0)));

        ResponseEntity<Page<RoleViewDTO>> response =
                roleController.findAll(userPrincipal, filters, pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getTotalElements());
        assertEquals(entity.getId(), response.getBody().getContent().getFirst().getId());
        assertEquals(entity.getCode(), response.getBody().getContent().getFirst().getCode());
        assertEquals(entity.getName(), response.getBody().getContent().getFirst().getName());

        verify(roleService).findAll(userPrincipal, filters, pageable);
        verify(roleMapper).toDTO(entity);
    }

    @Test
    @DisplayName("Should return role by ID with 200")
    void testFindById_shouldReturn200WithRole() {
        var entity = createSampleViewEntity();
        var dto = RoleViewDTO.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .extraParameters(entity.getExtraParameters())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .insertDate(entity.getInsertDate())
                .updateDate(entity.getUpdateDate())
                .build();

        when(roleService.findById(userPrincipal, entity.getId())).thenReturn(entity);
        when(roleMapper.toDTO(entity)).thenReturn(dto);

        ResponseEntity<RoleViewDTO> response =
                roleController.findById(userPrincipal, entity.getId());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(entity.getId(), response.getBody().getId());
        assertEquals(entity.getCode(), response.getBody().getCode());
        assertEquals(entity.getName(), response.getBody().getName());

        verify(roleService).findById(userPrincipal, entity.getId());
        verify(roleMapper).toDTO(entity);
    }

    @Test
    @DisplayName("Should propagate 404 when role does not exist")
    void testFindById_shouldPropagateNotFound() {
        UUID id = UUID.randomUUID();

        var exception = new ApiException(
                404,
                I18nMessage.of(
                        "error.role.not_found",
                        Map.of("id", id.toString())
                )
        );

        when(roleService.findById(userPrincipal, id)).thenThrow(exception);

        var thrown = assertThrows(
                ApiException.class,
                () -> roleController.findById(userPrincipal, id)
        );

        assertEquals(404, thrown.getStatusCode());
        verify(roleService).findById(userPrincipal, id);
        verify(roleMapper, never()).toDTO(any(RoleView.class));
    }

    @Test
    @DisplayName("Should update role and return 200 with RoleViewDTO")
    void testUpdate_shouldReturn200WithRoleViewDTO() {
        UUID id = UUID.randomUUID();
        var record = new RoleRecord(
                "UPDATED_ROLE",
                "Updated role",
                "Updated description",
                Map.of("scope", "updated")
        );
        var entity = createSampleViewEntity();
        var dto = RoleViewDTO.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .extraParameters(entity.getExtraParameters())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .insertDate(entity.getInsertDate())
                .updateDate(entity.getUpdateDate())
                .build();

        when(roleService.update(userPrincipal, id, record)).thenReturn(entity);
        when(roleMapper.toDTO(entity)).thenReturn(dto);

        ResponseEntity<RoleViewDTO> response =
                roleController.update(userPrincipal, id, record);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(entity.getId(), response.getBody().getId());
        assertEquals(entity.getCode(), response.getBody().getCode());
        assertEquals(entity.getName(), response.getBody().getName());

        verify(roleService).update(userPrincipal, id, record);
        verify(roleMapper).toDTO(entity);
    }

    @Test
    @DisplayName("Should propagate 404 when updating an unknown role")
    void testUpdate_shouldPropagateNotFound() {
        UUID id = UUID.randomUUID();
        var record = createSampleRecord();

        var exception = new ApiException(
                404,
                I18nMessage.of(
                        "error.role.not_found",
                        Map.of("id", id.toString())
                )
        );

        when(roleService.update(userPrincipal, id, record)).thenThrow(exception);

        var thrown = assertThrows(
                ApiException.class,
                () -> roleController.update(userPrincipal, id, record)
        );

        assertEquals(404, thrown.getStatusCode());
        verify(roleService).update(userPrincipal, id, record);
        verify(roleMapper, never()).toDTO(any(RoleView.class));
    }

    @Test
    @DisplayName("Should delete role and return 204")
    void testDeleteById_shouldReturn204() {
        UUID id = UUID.randomUUID();

        ResponseEntity<Void> response =
                roleController.deleteById(userPrincipal, id);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(roleService).deleteById(userPrincipal, id);
    }

    @Test
    @DisplayName("Should propagate 404 when deleting an unknown role")
    void testDeleteById_shouldPropagateNotFound() {
        UUID id = UUID.randomUUID();

        doThrow(new ApiException(
                404,
                I18nMessage.of(
                        "error.role.not_found",
                        Map.of("id", id.toString())
                )
        )).when(roleService).deleteById(userPrincipal, id);

        var thrown = assertThrows(
                ApiException.class,
                () -> roleController.deleteById(userPrincipal, id)
        );

        assertEquals(404, thrown.getStatusCode());
        verify(roleService).deleteById(userPrincipal, id);
    }
}
