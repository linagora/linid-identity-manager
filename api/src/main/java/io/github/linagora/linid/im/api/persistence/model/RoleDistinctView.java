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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
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
 * Read-only entity mapped to the {@code roles_view} database view, returning each role once.
 *
 * <p>
 * Omits the {@code organizational_unit_id} column so that a role held in several organizational units is
 * projected as a single row. The organizational units are exposed as a comma-separated list of names.
 * </p>
 *
 * <p>Audit information such as {@code createdBy}, {@code updatedBy}, {@code insertDate}, and {@code updateDate}
 * is inherited from {@link AbstractViewEntity}.
 */
@Entity
@Table(name = "roles_view")
@Data
@Immutable
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RoleDistinctView extends AbstractViewEntity {

    /**
     * Unique identifier of the role.
     */
    @Id
    @Column(name = "rol_id")
    private UUID id;

    /**
     * Unique technical identifier of the role.
     */
    @Column(name = "code", nullable = false)
    private String code;

    /**
     * Human-readable name of the role.
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Optional description of the role.
     */
    @Column(name = "description")
    private String description;

    /**
     * Names of the organizational units in which the role is held, represented as a comma-separated list.
     * {@code null} when the role is not held anywhere.
     */
    @Column(name = "organizational_units")
    private String organizationalUnits;

    /**
     * Whether the role can be deleted, that is when no account holds it in any organizational unit.
     */
    @Column(name = "deletable", nullable = false)
    private boolean deletable;

    /**
     * Additional deployment-specific attributes stored as JSONB.
     */
    @Column(name = "extra_parameters", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> extraParameters;

    /**
     * Creates a role view with its audit information, identity information and organizational units.
     *
     * <p>This constructor is intentionally provided with all view properties as parameters to allow
     * {@code spring-query-filter} to instantiate the entity when creating filtered query projections.
     *
     * @param createdBy           the full name of the account that created the role.
     * @param updatedBy           the full name of the account that last updated the role.
     * @param insertDate          the timestamp when the role was created.
     * @param updateDate          the timestamp when the role was last updated.
     * @param id                  the unique identifier of the role.
     * @param code                the unique technical identifier of the role.
     * @param name                the human-readable name of the role.
     * @param description         the optional description of the role.
     * @param organizationalUnits the names of the organizational units in which the role is held, represented as a
     *                            comma-separated list.
     * @param extraParameters     additional deployment-specific attributes stored as JSON.
     * @param deletable           whether the role can be deleted.
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public RoleDistinctView(final String createdBy,
                            final String updatedBy,
                            final OffsetDateTime insertDate,
                            final OffsetDateTime updateDate,
                            final UUID id,
                            final String code,
                            final String name,
                            final String description,
                            final String organizationalUnits,
                            final Map<String, Object> extraParameters,
                            final boolean deletable) {
        super(createdBy, updatedBy, insertDate, updateDate);
        this.id = id;
        this.code = code;
        this.name = name;
        this.description = description;
        this.organizationalUnits = organizationalUnits;
        this.extraParameters = extraParameters;
        this.deletable = deletable;
    }
}
