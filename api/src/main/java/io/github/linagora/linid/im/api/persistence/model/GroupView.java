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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
 * Read-only representation of a group enriched with the label of its parent group and the names of its
 * associated organizational unit and application, backed by the {@code groups_view} database view.
 */
@Entity
@Immutable
@Table(name = "groups_view")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@QueryFilter
public class GroupView extends AbstractViewEntity {

    /**
     * Unique identifier of the group (UUID).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "grp_id")
    @FilterType(type = UUID.class)
    @QueryFilterField(type = UUID.class, description = "Group unique identifier")
    private UUID id;

    /**
     * Functional unique identifier of the group.
     */
    @Column(name = "code", nullable = false)
    @FilterType(type = String.class)
    @QueryFilterField(type = String.class, description = "Functional unique identifier of the group")
    private String code;

    /**
     * Human-readable label of the group.
     */
    @Column(name = "label", nullable = false)
    @FilterType(type = String.class)
    @QueryFilterField(type = String.class, description = "Human-readable label of the group")
    private String label;

    /**
     * Identifier of the parent group.
     */
    @Column(name = "parent_id")
    @FilterType(type = UUID.class)
    @QueryFilterField(type = UUID.class, description = "Identifier of the parent group")
    private UUID parentId;

    /**
     * Label of the parent group.
     */
    @Column(name = "parent_label")
    @FilterType(type = String.class)
    @QueryFilterField(type = String.class, description = "Label of the parent group")
    private String parentLabel;

    /**
     * Free-text description of the group.
     */
    @Column(name = "description")
    @FilterType(type = String.class)
    @QueryFilterField(type = String.class, description = "Free-text description of the group")
    private String description;

    /**
     * Email address of the group.
     */
    @Column(name = "email")
    @FilterType(type = String.class)
    @QueryFilterField(type = String.class, description = "Email address of the group")
    private String email;

    /**
     * Identifier of the organizational unit associated with the group.
     */
    @Column(name = "oun_id")
    @FilterType(type = UUID.class)
    @QueryFilterField(type = UUID.class, description = "Identifier of the organizational unit associated with the"
        + " group")
    private UUID organizationalUnitId;

    /**
     * Name of the organizational unit associated with the group.
     */
    @Column(name = "organizational_unit_name")
    @FilterType(type = String.class)
    @QueryFilterField(type = String.class, description = "Name of the organizational unit associated with the group")
    private String organizationalUnitName;

    /**
     * Identifier of the application associated with the group.
     */
    @Column(name = "app_id")
    @FilterType(type = UUID.class)
    @QueryFilterField(type = UUID.class, description = "Identifier of the application associated with the group")
    private UUID applicationId;

    /**
     * Name of the application associated with the group.
     */
    @Column(name = "application_name")
    @FilterType(type = String.class)
    @QueryFilterField(type = String.class, description = "Name of the application associated with the group")
    private String applicationName;

    /**
     * Additional deployment-specific attributes stored as JSON.
     * <p>
     * This field allows integrators and customers to extend the standard data model
     * with custom parameters required by their environment without modifying the
     * application schema.
     */
    @Column(name = "extra_parameters", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> extraParameters;
}
