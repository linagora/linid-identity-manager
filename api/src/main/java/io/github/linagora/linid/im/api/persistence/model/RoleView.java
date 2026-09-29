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
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Read-only entity mapped to the {@code roles_view} database view.
 *
 * <p>
 * Provides role information enriched with the human-readable names of the
 * creator and last updater accounts, together with the organizational units in which the role is held.
 * One row is returned for each organizational unit in which the role is held.
 * </p>
 */
@Entity
@Table(name = "roles_view")
@Data
@Immutable
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@QueryFilter
public class RoleView extends AbstractViewEntity {

    /**
     * Unique identifier of the role.
     */
    @Id
    @Column(name = "rol_id")
    @FilterType(type = UUID.class)
    @QueryFilterField(type = UUID.class, description = "Role unique identifier")
    private UUID id;

    /**
     * Unique technical identifier of the role.
     */
    @Column(name = "code", nullable = false)
    @FilterType(type = String.class)
    @QueryFilterField(type = String.class, description = "Role code")
    private String code;

    /**
     * Human-readable name of the role.
     */
    @Column(name = "name", nullable = false)
    @FilterType(type = String.class)
    @QueryFilterField(type = String.class, description = "Role name")
    private String name;

    /**
     * Optional description of the role.
     */
    @Column(name = "description")
    @FilterType(type = String.class)
    @QueryFilterField(type = String.class, description = "Role description")
    private String description;

    /**
     * Identifier of an organizational unit in which the role is held by at least one account.
     * {@code null} when the role is not held anywhere.
     */
    @Column(name = "organizational_unit_id")
    @FilterType(type = UUID.class)
    @QueryFilterField(
        type = UUID.class,
        description = "Unique identifier of an organizational unit in which the role is held"
    )
    private UUID organizationalUnitId;

    /**
     * Names of the organizational units in which the role is held, represented as a comma-separated list.
     * {@code null} when the role is not held anywhere.
     */
    @Column(name = "organizational_units")
    @FilterType(type = String.class)
    @QueryFilterField(
        type = String.class,
        description = "Names of the organizational units in which the role is held, represented as a "
            + "comma-separated list"
    )
    private String organizationalUnits;

    /**
     * Whether the role can be deleted, that is when no account holds it in any organizational unit.
     */
    @Column(name = "deletable", nullable = false)
    @FilterType(type = Boolean.class)
    @QueryFilterField(type = Boolean.class, description = "Whether the role can be deleted (held by no account)")
    private boolean deletable;

    /**
     * Additional deployment-specific attributes stored as JSONB.
     */
    @Column(name = "extra_parameters", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> extraParameters;
}
