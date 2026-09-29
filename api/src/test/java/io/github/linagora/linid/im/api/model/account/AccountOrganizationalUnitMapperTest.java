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

package io.github.linagora.linid.im.api.model.account;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.linagora.linid.im.api.persistence.model.AccountOrganizationalUnitView;
import io.github.linagora.linid.im.api.persistence.model.OrganizationalUnitStatusEnum;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Test class: AccountOrganizationalUnitMapper")
class AccountOrganizationalUnitMapperTest {

    private AccountOrganizationalUnitMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new AccountOrganizationalUnitMapperImpl();
    }

    @Test
    @DisplayName("Should map the organizational unit, relationship and audit information of the view")
    void testToDTO_shouldMapAllFields() {
        UUID id = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        OffsetDateTime insertDate = OffsetDateTime.now().minusDays(1);
        OffsetDateTime updateDate = OffsetDateTime.now();
        AccountOrganizationalUnitView view = AccountOrganizationalUnitView.builder()
            .id(id)
            .accountId(UUID.randomUUID())
            .name("Headquarters")
            .type("DIVISION")
            .status(OrganizationalUnitStatusEnum.ACTIVE)
            .relationExtraParameters(Map.of("role", "member"))
            .roleId(roleId)
            .roleName("Manager")
            .createdBy("Admin User")
            .updatedBy("Other User")
            .insertDate(insertDate)
            .updateDate(updateDate)
            .build();

        AccountOrganizationalUnitViewDTO dto = mapper.toDTO(view);

        assertEquals(id, dto.getId());
        assertEquals("Headquarters", dto.getName());
        assertEquals("DIVISION", dto.getType());
        assertEquals(OrganizationalUnitStatusEnum.ACTIVE, dto.getStatus());
        assertEquals(Map.of("role", "member"), dto.getRelationExtraParameters());
        assertEquals(roleId, dto.getRoleId());
        assertEquals("Manager", dto.getRoleName());
        assertEquals("Admin User", dto.getCreatedBy());
        assertEquals("Other User", dto.getUpdatedBy());
        assertEquals(insertDate, dto.getInsertDate());
        assertEquals(updateDate, dto.getUpdateDate());
    }

    @Test
    @DisplayName("Should return null when the view is null")
    void testToDTO_shouldReturnNullOnNullView() {
        assertNull(mapper.toDTO(null));
    }
}
