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

package io.github.linagora.linid.im.api.persistence.model;

import io.github.zorin95670.predicate.FilterType;
import io.github.zorin95670.processor.annotation.QueryFilter;
import io.github.zorin95670.processor.annotation.QueryFilterField;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Immutable;

/**
 * Read-only projection of a manager of an organizational unit.
 *
 * <p>This entity is mapped to the {@code organizational_unit_managers_view} database view, which holds
 * one row per (organizational unit, account) pair, enriched with the identity attributes of the
 * manager account.</p>
 *
 * <p>The identifier is {@code oum_id}, the identifier of the manager record: it is unique across all
 * organizational units. The manager account is carried by {@link #accountId}.</p>
 *
 * <p>This entity is immutable and intended strictly for read operations; persistence changes must be
 * performed through {@link OrganizationalUnitManager}.</p>
 */
@Entity
@Table(name = "organizational_unit_managers_view")
@Data
@Immutable
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@QueryFilter
public class OrganizationalUnitManagerView extends AbstractViewEntity {

    /**
     * Identifier of the organizational unit manager (UUID).
     */
    @Id
    @Column(name = "oum_id")
    @FilterType(type = UUID.class)
    @QueryFilterField(type = UUID.class, description = "Organizational unit manager identifier")
    private UUID id;

    /**
     * Identifier of the managed organizational unit (UUID).
     */
    @Column(name = "oun_id")
    @FilterType(type = UUID.class)
    @QueryFilterField(type = UUID.class, description = "Organizational unit identifier")
    private UUID organizationalUnitId;

    /**
     * Identifier of the manager account (UUID).
     */
    @Column(name = "act_id")
    @FilterType(type = UUID.class)
    @QueryFilterField(type = UUID.class, description = "Manager account identifier")
    private UUID accountId;

    /**
     * Last name of the manager.
     */
    @Column(name = "lastname")
    @FilterType(type = String.class)
    @QueryFilterField(type = String.class, description = "Last name of the manager")
    private String lastname;

    /**
     * First name of the manager.
     */
    @Column(name = "firstname")
    @FilterType(type = String.class)
    @QueryFilterField(type = String.class, description = "First name of the manager")
    private String firstname;

    /**
     * Email address of the manager account.
     */
    @Column(name = "email")
    @FilterType(type = String.class)
    @QueryFilterField(type = String.class, description = "Email address of the manager")
    private String email;
}
