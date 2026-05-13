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

import io.github.linagora.linid.im.api.model.organizationalunit.OrganizationalUnitMapper;
import io.github.linagora.linid.im.api.model.organizationalunit.OrganizationalUnitRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.OrganizationalUnit;
import io.github.linagora.linid.im.api.persistence.model.OrganizationalUnitQueryFilterDto;
import io.github.linagora.linid.im.api.service.OrganizationalUnitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: OrganizationalUnitController")
class OrganizationalUnitControllerTest {

    @Mock
    private OrganizationalUnitService service;

    @Mock
    private OrganizationalUnitMapper mapper;

    @Mock
    private PagedResponseStatusResolver pagedResponseStatusResolver;

    @InjectMocks
    private OrganizationalUnitController controller;

    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        userPrincipal = new UserPrincipal();
        userPrincipal.setId(UUID.randomUUID());
        userPrincipal.setEmail("admin@example.com");
    }

    @Test
    @DisplayName("Should create organizational unit")
    void testCreate() {
        when(service.create(any(), any())).thenReturn(new OrganizationalUnit());
        var record = new OrganizationalUnitRecord(UUID.randomUUID(), "test", "test");

        var response = controller.create(userPrincipal, record);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    @DisplayName("Should find organization units")
    void testFindAll() {
        when(service.findAll(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(pagedResponseStatusResolver.resolve(any())).thenReturn(ResponseEntity.ok(new PageImpl<>(List.of())));

        var response = controller.findAll(userPrincipal, new OrganizationalUnitQueryFilterDto(), Pageable.unpaged());

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("Should find organizational unit by id")
    void testFindById() {
        when(service.findById(any(), any())).thenReturn(new OrganizationalUnit());

        var response = controller.findById(userPrincipal, UUID.randomUUID());

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("Should update organizational unit")
    void testUpdate() {
        when(service.update(any(), any(), any())).thenReturn(new OrganizationalUnit());

        var response = controller.update(userPrincipal, UUID.randomUUID(), new OrganizationalUnitRecord(UUID.randomUUID(), "test", "test"));

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("Should delete organizational unit by id")
    void testDeleteById() {
        doNothing().when(service).deleteById(any(), any());

        var response = controller.deleteById(userPrincipal, UUID.randomUUID());

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
