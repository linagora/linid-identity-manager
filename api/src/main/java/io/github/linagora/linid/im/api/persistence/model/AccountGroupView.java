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
 * Read-only projection of a group as seen from the account attached to it.
 *
 * <p>This entity is mapped to the {@code account_groups_view} database view, which holds one row per
 * (account, group) pair and excludes accounts without any membership.</p>
 *
 * <p>The identifier is {@code grp_id} rather than {@code act_id}: listing the groups of a single
 * account yields several rows sharing the same {@code act_id}, and an entity identified by the
 * account would resolve every one of those rows to the same instance, returning the first group
 * repeated. Note that {@code grp_id} is only unique within a single account, so this projection must
 * always be queried with an account filter.</p>
 *
 * <p>This entity is immutable and intended strictly for read operations.</p>
 */
@Entity
@Table(name = "account_groups_view")
@Data
@Immutable
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@QueryFilter
public class AccountGroupView extends AbstractViewEntity {

    /**
     * Identifier of the group (UUID).
     */
    @Id
    @Column(name = "grp_id")
    @FilterType(type = UUID.class)
    @QueryFilterField(type = UUID.class, description = "Group identifier")
    private UUID id;

    /**
     * Identifier of the account attached to the group (UUID).
     */
    @Column(name = "act_id")
    @FilterType(type = UUID.class)
    @QueryFilterField(type = UUID.class, description = "Account identifier")
    private UUID accountId;

    /**
     * Functional unique identifier of the group.
     */
    @Column(name = "code")
    @FilterType(type = String.class)
    @QueryFilterField(type = String.class, description = "Functional unique identifier of the group")
    private String code;

    /**
     * Human-readable name of the group.
     */
    @Column(name = "name")
    @FilterType(type = String.class)
    @QueryFilterField(type = String.class, description = "Human-readable name of the group")
    private String name;

    /**
     * Identifier of the parent group.
     */
    @Column(name = "parent_id")
    @FilterType(type = UUID.class)
    @QueryFilterField(type = UUID.class, description = "Identifier of the parent group")
    private UUID parentId;

    /**
     * Name of the parent group.
     */
    @Column(name = "parent_name")
    @FilterType(type = String.class)
    @QueryFilterField(type = String.class, description = "Name of the parent group")
    private String parentName;

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
    @QueryFilterField(type = UUID.class,
        description = "Identifier of the organizational unit associated with the group")
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
     * Additional deployment-specific attributes associated with the relationship between the account
     * and the group.
     */
    @Column(name = "relation_extra_parameters", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> relationExtraParameters;
}
